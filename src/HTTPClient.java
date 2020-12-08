import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;

public class HTTPClient {
	public final static String ADDRESS_SCHEMA = "(?i)(https?://)?([-a-zA-Z0-9@:%._+~#=]{1,256}" +
												"\\.[a-zA-Z0-9()]{1,6}\\b)([-a-zA-Z0-9()@:%_+.~#?&/=]*)";
	public final static int DEFAULT_PORT = 80;
	public final static String CRLF = "\r\n";
	private int studentId;
	HashMap<String, String> queries;
	
	public HTTPClient() {
		this.studentId = 96243031;
	}
	
	public static Object[] initSocket(String hostName) {
		try {
			var socket = new Socket(hostName, DEFAULT_PORT);
			var outputStream = socket.getOutputStream();
			var inputStream = socket.getInputStream();
			return new Object[]{socket, outputStream, inputStream};
		} catch (IOException e) {
			System.out.println("Failed while initializing socket and streams!");
			return null;
		}
	}
	
	public void connect(Matcher matcher, String method, HashMap<String, String> queries) {
		this.queries = queries;
		String scheme = matcher.group(1);
		String hostName = matcher.group(2);
		String path = matcher.group(3);
		if (path.equals("")) {
			path = "/";
		}
		
		/*
		 * 51.89.222.172/saat.html
		 * 51.89.222.172/dark_s03e01.mkv
		 * 51.89.222.172
		 * 51.89.222.172/status
		 * 51.89.222.172/test.html
		 * 51.89.222.172/sample-json
		 * 51.89.222.172/sample-error1
		 * 51.89.222.172/sample-error1-with-body
		 * 51.89.222.172/sample-error2
		 * 51.89.222.172/sample-error3
		 */
		
		if (getExtFromPath(path).equals("mkv")) {
			handleMkvFile(hostName, path);
			return;
		}
		
		Object[] socketStreams = initSocket(hostName);
		assert socketStreams != null;
		var socket = (Socket) socketStreams[0];
		var outputStream = (OutputStream) socketStreams[1];
		var inputStream = (InputStream) socketStreams[2];
		
		var request = new HTTPRequest(method, path, hostName, studentId, queries);
		try {
			assert outputStream != null;
			request.send(outputStream);
		} catch (IOException e) {
			System.out.println("Failed while sending request!");
		}
		
		System.out.println("===== Finished sending the request. =====");
		
		var response = new HTTPResponse(path, hostName);
		try {
			assert inputStream != null;
			response.receive(inputStream);
		} catch (IOException e) {
			System.out.println("Failed while receiving the response!");
		}
		
		var resources = response.parseResponse();
		
		destroySocket(socketStreams);
		
		if (resources.size() > 0) {
			downloadExternalResources(resources);
		}
	}
	
	private void handleMkvFile(String hostName, String path) {
		long downloadMB = 5;
		int numDownloaders = 5;
		long MB = 1024 * 1024;
		long eachThreadMB = (downloadMB / numDownloaders) * MB;
		var threads = new Downloader[numDownloaders];
		for (int i = 0; i < numDownloaders; i++) {
			long start = i * eachThreadMB;
			long end = (i + 1) * eachThreadMB;
			threads[i] = new Downloader(hostName, path, studentId, start, end, i);
			threads[i].start();
		}

		waitForThreads(threads);
		
		try {
			glueMkvParts(path, numDownloaders);
		} catch (IOException e) {
			System.out.println("Failed while gluing mkv parts together!");
		}
		
		deleteTempParts(path, numDownloaders);
	}
	
	private void deleteTempParts(String path, int numDownloaders) {
		String destinationFileName = "Files/" + getFileNameFromPath(path);
		for (int i = 0; i < numDownloaders; i++) {
			String fileName = "Files/" + i + "_" + destinationFileName.split("/", -1)[1];
			
			var file = new File(fileName);
			file.delete();
		}
	}
	
	private void glueMkvParts(String path, int numDownloaders) throws IOException {
		String destinationFileName = "Files/" + getFileNameFromPath(path);
		File file = new File(destinationFileName);
		var destinationOutputStream = new FileOutputStream(file, true);
		
		for (int i = 0; i < numDownloaders; i++) {
			String fileName = "Files/" + i + "_" + destinationFileName.split("/", -1)[1];
			var in = new FileInputStream(fileName);
			
			int byteRead;
			while ((byteRead = in.read()) != -1) {
				destinationOutputStream.write(byteRead);
			}
		}
	}
	
	private void waitForThreads(Downloader[] threads) {
		for (Downloader thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				System.out.println("Failed while waiting for all threads!");
			}
		}
	}
	
	private void downloadExternalResources(ArrayList<Resource> resources) {
		// delete www.radcom.co and fa.wikiquote.org
		var bannedHosts = new ArrayList<String>();
		bannedHosts.add("www.radcom.co");
		bannedHosts.add("fa.wikiquote.org");
		
		for (Resource resource : resources) {
			var path = resource.getPath();
			var host = resource.getHostName();
			if (!getExtFromPath(path).equals("html") && !bannedHosts.contains(host)) {
				var socketStreams = initSocket(host);
				assert socketStreams != null;
				handleResource(socketStreams, path, host);
				destroySocket(socketStreams);
			}
		}
	}
	
	private void handleResource(Object[] socketStreams, String path, String hostName) {
		var outputStream = (OutputStream) socketStreams[1];
		var inputStream = (InputStream) socketStreams[2];
		
		var request = new HTTPRequest("GET", path, hostName, studentId, null);
		try {
			request.send(outputStream);
		} catch (IOException e) {
			System.out.println("Failed while sending request!");
		}
		
		System.out.println("===== Finished sending the request. =====");
		
		var response = new HTTPResponse(path, hostName);
		try {
			response.receive(inputStream);
		} catch (IOException e) {
			System.out.println("Failed while receiving the response");
		}
		response.parseResponse();
		System.out.println("===== Finished receiving the response. =====");
	}
	
	public static String getFileNameFromPath(String path) {
		var pathSplit = path.split("/", -1);
		return pathSplit[pathSplit.length - 1];
	}
	
	public static String getCurrentDirFromPath(String path) {
		var pathSplit = path.split("/", -1);
		return String.join("/", Arrays.asList(pathSplit).subList(0, pathSplit.length - 1)) + "/";
	}
	
	private String getExtFromPath(String path) {
		String name = getFileNameFromPath(path);
		var nameSplit = name.split("\\.");
		return nameSplit[nameSplit.length - 1];
	}
	
	public static boolean isAbsoluteURL(String url) {
		return url.matches(HTTPClient.ADDRESS_SCHEMA);
	}
	
	public static void destroySocket(Object[] socketStreams) {
		try {
			((Socket) socketStreams[0]).close();
			((OutputStream) socketStreams[1]).close();
			((InputStream) socketStreams[2]).close();
		} catch (IOException e) {
			System.out.println("Failed while destroying the socket and the streams!");
		}
	}
	
	public void setStudentId(int studentId) {
		this.studentId = studentId;
	}
}
