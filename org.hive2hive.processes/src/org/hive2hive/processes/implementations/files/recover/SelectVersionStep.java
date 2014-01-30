package org.hive2hive.processes.implementations.files.recover;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hive2hive.core.exceptions.Hive2HiveException;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.model.FileTreeNode;
import org.hive2hive.core.model.FileVersion;
import org.hive2hive.core.model.IFileVersion;
import org.hive2hive.core.model.MetaDocument;
import org.hive2hive.core.model.MetaFile;
import org.hive2hive.core.model.UserProfile;
import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.network.data.UserProfileManager;
import org.hive2hive.core.process.recover.IVersionSelector;
import org.hive2hive.processes.ProcessFactory;
import org.hive2hive.processes.framework.RollbackReason;
import org.hive2hive.processes.framework.abstracts.ProcessComponent;
import org.hive2hive.processes.framework.abstracts.ProcessStep;
import org.hive2hive.processes.framework.exceptions.InvalidProcessStateException;
import org.hive2hive.processes.framework.interfaces.IProcessComponentListener;
import org.hive2hive.processes.implementations.context.RecoverFileContext;

/**
 * Asks the user which version to restore and initiates the restore steps
 * 
 * @author Nico
 * 
 */
public class SelectVersionStep extends ProcessStep {

	private final static Logger logger = H2HLoggerFactory.getLogger(SelectVersionStep.class);
	private final RecoverFileContext context;
	private final IVersionSelector selector;
	private final NetworkManager networkManager;

	public SelectVersionStep(RecoverFileContext context, IVersionSelector selector,
			NetworkManager networkManager) {
		this.context = context;
		this.selector = selector;
		this.networkManager = networkManager;
	}

	@Override
	protected void doExecute() throws InvalidProcessStateException {
		MetaDocument metaDocument = context.consumeMetaDocument();
		if (metaDocument == null) {
			cancel(new RollbackReason(this, "Meta document not found"));
			return;
		} else if (!(metaDocument instanceof MetaFile)) {
			cancel(new RollbackReason(this, "Meta document is not a meta file"));
			return;
		}

		MetaFile metaFile = (MetaFile) metaDocument;

		// cast the versions to the public interface
		List<IFileVersion> versions = new ArrayList<IFileVersion>();
		for (FileVersion version : metaFile.getVersions()) {
			if (metaFile.getNewestVersion().equals(version)) {
				// skip newest version since it's not worth to restore it
				continue;
			}

			versions.add(version);
		}

		logger.debug("Start with the selection of the version by the user. He has choice between "
				+ versions.size() + " versions");
		IFileVersion selected = selector.selectVersion(versions);
		if (selected == null) {
			cancel(new RollbackReason(this, "Selected file version is null"));
			return;
		}

		// find the selected version
		FileVersion selectedVersion = null;
		for (FileVersion version : metaFile.getVersions()) {
			if (version.getIndex() == selected.getIndex()) {
				selectedVersion = version;
				break;
			}
		}

		// check if the developer returned an invalid index
		if (selectedVersion == null) {
			cancel(new RollbackReason(this, "Invalid version index selected"));
			return;
		}

		logger.debug("Selected version " + selected.getIndex() + " where "
				+ metaFile.getNewestVersion().getIndex() + " is newest");

		// 1. download the file with new name <filename>_<date>
		// 2. add the file with an AddFileProcess (which also notifies other clients)
		try {
			// find the node at the user profile
			UserProfileManager profileManager = networkManager.getSession().getProfileManager();
			UserProfile userProfile = profileManager.getUserProfile(getID(), false);
			FileTreeNode selectedNode = userProfile.getFileById(metaFile.getId());
			if (selectedNode == null) {
				throw new Hive2HiveException("File node not found");
			}

			// generate a new file name indicating that the file is restored
			Date versionDate = new Date(selected.getDate());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
			String newFileName = context.getFile().getName() + "_" + sdf.format(versionDate);
			logger.debug("Starting to download the restored file under the name '" + newFileName + "'");

			File destination = new File(context.getFile().getParentFile(), newFileName);

			// start the process to download the files
			ProcessComponent downloadProcess = ProcessFactory.instance().createDownloadFileProcess(
					selectedNode.getFileKey(), selected.getIndex(), destination, networkManager);
			downloadProcess.attachListener(new StartAddRestoredFileListener(destination));
			downloadProcess.start();
		} catch (Hive2HiveException e) {
			cancel(new RollbackReason(this, e.getMessage()));
		}

	}

	/**
	 * Listener that is called as soon the download of the restored version is done
	 * 
	 * @author Nico
	 * 
	 */
	private class StartAddRestoredFileListener implements IProcessComponentListener {

		private final File destination;

		public StartAddRestoredFileListener(File destination) {
			this.destination = destination;
		}

		@Override
		public void onSucceeded() {
			logger.debug("Downloading of the restored version finished. Continue with adding it as an own file");
			try {
				ProcessComponent process = ProcessFactory.instance().createNewFileProcess(destination,
						networkManager);
				process.attachListener(new FinalizeRestoreListener());
				process.start();
			} catch (Hive2HiveException e) {
				try {
					cancel(new RollbackReason(SelectVersionStep.this, e.getMessage()));
				} catch (InvalidProcessStateException e1) {
					logger.error("Double exception: Could not start the rollback.", e);
				}
			}
		}

		@Override
		public void onFailed() {
			try {
				cancel(new RollbackReason(SelectVersionStep.this, "Could not download the restored version"));
			} catch (InvalidProcessStateException e) {
				logger.error("Double exception: Could not start the rollback", e);
			}
		}

		@Override
		public void onFinished() {
			// ignore
		}
	}

	/**
	 * Listener that is called as soon as the restored file is uploaded to the DHT
	 * 
	 * @author Nico
	 * 
	 */
	private class FinalizeRestoreListener implements IProcessComponentListener {

		@Override
		public void onSucceeded() {
			logger.debug("Successfully restored the file version");
		}

		@Override
		public void onFailed() {
			try {
				cancel(new RollbackReason(SelectVersionStep.this,
						"Could not upload the restored file to the DHT"));
			} catch (InvalidProcessStateException e) {
				logger.error("Double exception: Could not start the rollback", e);
			}
		}

		@Override
		public void onFinished() {
			// ignore
		}
	}
}
