
/**
 * Proxy.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.*;
import java.nio.file.OpenOption;
import java.rmi.Naming;

class Proxy {

	private static Cache cache = null;
	private static ServerOperations server = null;

	private static class FileHandler implements FileHandling {

		private FDTable fdTable;

		private CloseHandler closeHandler;
		private WriteHandler writeHandler;
		private ReadHandler readHandler;
		private LseekHandler lseekHandler;
		private UnlinkHandler unlinkHandler;

		public FileHandler() {
			fdTable = new FDTable();
			closeHandler = null;
			writeHandler = null;
			readHandler = null;
			lseekHandler = null;
		}

		public int open(String path, OpenOption o) {
			Logger.log("Proxy: open(" + path + ", " + o + ")");
			CheckOption checkOption;
			String mode = "";
			Boolean canRead = false;
			Boolean canWrite = false;
			switch (o) {
				case CREATE:
					checkOption = CheckOption.CREATE;
					mode = "rw";
					canRead = true;
					canWrite = true;
					break;
				case CREATE_NEW:
					checkOption = CheckOption.CREATE_NEW;
					mode = "rw";
					canRead = true;
					canWrite = true;
					break;
				case READ:
					checkOption = CheckOption.READ;
					mode = "r";
					canRead = true;
					break;
				case WRITE:
					checkOption = CheckOption.WRITE;
					mode = "w";
					canWrite = true;
					break;
				default:
					return Errno.EINVAL;
			}

			CheckResult checkResult = null;
			try {
				checkResult = server.check(path, checkOption);
			} catch (Exception e) {
				System.out.println(e);
				System.exit(-1);
			}

			if (checkResult.getResult() != 0) {
				return checkResult.getResult();
			}
			String localPath = null;
			if (!checkResult.getIsDirectory() && !checkResult.getExists()) {
				localPath = cache.generateLocalPath(checkResult.getNormalizedPath());
				File localFile = new File(localPath);
				try {
					localFile.createNewFile();
				} catch (IOException e) {
					return Errno.EACCES;
				}
			} else {
				localPath = cache.getLocalFilePath(checkResult.getNormalizedPath());
				if (localPath == null && !checkResult.getIsDirectory()) {
					byte[] fileData = null;
					try {
						fileData = server.getFile(checkResult.getNormalizedPath());

					} catch (Exception e) {
						System.out.println(e);
						System.exit(-1);
					}
					if (fileData == null) {
						return Errno.EACCES;
					}
					localPath = cache.generateLocalPath(checkResult.getNormalizedPath());
					File localFile = new File(localPath);
					try {
						localFile.createNewFile();
						FileOutputStream fos = new FileOutputStream(localFile);
						fos.write(fileData);
						fos.close();
					} catch (IOException e) {
						return Errno.EACCES;
					}
					cache.addCacheItem(checkResult.getNormalizedPath(), localPath, checkResult.getVersion(),
							checkResult.getIsDirectory(), checkResult.getCanRead(), checkResult.getCanWrite());
				}
			}

			int fd = fdTable.getFreeFd();
			if (fd < 0) {
				return Errno.EMFILE;
			}

			File file = new File(localPath);
			try {
				RandomAccessFile randomAccessFile = null;
				if (!checkResult.getIsDirectory()) {
					randomAccessFile = new RandomAccessFile(file, mode);
				}
				OpenFile openFile = new OpenFile(randomAccessFile, canRead, canWrite, checkResult.getIsDirectory(),
						localPath, checkResult.getNormalizedPath());
				fdTable.addOpenFile(fd, openFile);
			} catch (FileNotFoundException e) {
				return Errno.EACCES;
			}

			Logger.log("Proxy: open(" + path + ", " + o + ") = " + fd);
			return fd;
		}

		public int close(int fd) {
			Logger.log("Proxy: close(" + fd + ")");
			if (!fdTable.verifyFd(fd)) {
				return Errno.EBADF;
			}

			OpenFile file = fdTable.getOpenFile(fd);
			if (file.hasModified()) {
				String serverPath = file.getServerPath();
				String localPath = file.getLocalPath();
				byte[] fileData = null;
				File localFile = new File(localPath);
				try {
					FileInputStream fis = new FileInputStream(localFile);
					fileData = new byte[(int) localFile.length()];
					fis.read(fileData);
					fis.close();
				} catch (IOException e) {
					System.out.println(e);
					System.exit(-1);
				}
				try {
					server.putFile(serverPath, fileData);
				} catch (Exception e) {
					System.out.println(e);
					System.exit(-1);
				}
			}
			try {
				file.close();
			} catch (IOException e) {
				System.out.println(e);
				System.exit(-1);
			}

			fdTable.removeOpenFile(fd);
			Logger.log("Proxy: close(" + fd + ") = " + 0);
			return 0;
		}

		public long write(int fd, byte[] buf) {
			Logger.log("Proxy: write(" + fd + ")");
			if (writeHandler == null) {
				writeHandler = new WriteHandler(fdTable);
			}
			long res = writeHandler.write(fd, buf);
			Logger.log("Proxy: write(" + fd + ") = " + res);
			return res;
		}

		public long read(int fd, byte[] buf) {
			Logger.log("Proxy: read(" + fd + ")");
			if (readHandler == null) {
				readHandler = new ReadHandler(fdTable);
			}
			long res = readHandler.read(fd, buf);
			Logger.log("Proxy: read(" + fd + ") = " + res);
			return res;
		}

		public long lseek(int fd, long pos, LseekOption o) {
			Logger.log("Proxy: lseek(" + fd + ")");
			if (lseekHandler == null) {
				lseekHandler = new LseekHandler(fdTable);
			}
			long res = lseekHandler.lseek(fd, pos, o);
			Logger.log("Proxy: lseek(" + fd + ") = " + res);
			return res;
		}

		public int unlink(String path) {
			Logger.log("Proxy: unlink(" + path + ")");
			if (unlinkHandler == null) {
				unlinkHandler = new UnlinkHandler(fdTable);
			}
			int res = unlinkHandler.unlink(path);
			Logger.log("Proxy: unlink(" + path + ") = " + res);
			return res;
		}

		public void clientdone() {
			return;
		}

	}

	public static Cache getCache() {
		return cache;
	}

	public static ServerOperations getServer() {
		return server;
	}

	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		String serverip = args[0];
		int serverport = Integer.parseInt(args[1]);
		String cacheDir = args[2];
		long cachesize = Long.parseLong(args[3]);
		String serverUrl = "//" + serverip + ":" + serverport + "/Server";
		try {
			server = (ServerOperations) Naming.lookup(serverUrl);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(-1);
		}
		cache = new Cache(cacheDir, cachesize);
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
