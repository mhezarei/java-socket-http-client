import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Downloader extends Thread {
	private final String hostName;
	private final String path;
	private final int studentId;
	private final long start;
	private final long end;
	private final int id;
	
	public Downloader(String hostName, String path, int studentId, long start, long end, int id) {
		this.hostName = hostName;
		this.path = path;
		this.studentId = studentId;
		this.start = start;
		this.end = end;
		this.id = id;
	}
	
	@Override
	public void run() {
		Object[] socketStreams = HTTPClient.initSocket(hostName);
		assert socketStreams != null;
		var socket = (Socket) socketStreams[0];
		var outputStream = (OutputStream) socketStreams[1];
		var inputStream = (InputStream) socketStreams[2];
		
		var request = new HTTPRequest("GET", path, hostName, studentId, start, end);
		try {
			assert outputStream != null;
			request.send(outputStream);
		} catch (IOException e) {
			System.out.println("Failed while sending request!");
		}
		
		System.out.println("===== Finished sending the request. =====");
		
		var response = new HTTPResponse(path, hostName, id);
		try {
			assert inputStream != null;
			response.receive(inputStream);
		} catch (IOException e) {
			System.out.println("Failed while receiving the response!");
		}
		
		response.parseResponse();
		
		HTTPClient.destroySocket(socketStreams);
	}
	
//	private Object[] initSocket(String hostName) {
//		try {
//			var socket = new Socket(hostName, HTTPClient.DEFAULT_PORT);
//			var outputStream = socket.getOutputStream();
//			var inputStream = socket.getInputStream();
//			return new Object[]{socket, outputStream, inputStream};
//		} catch (IOException e) {
//			System.out.println("Failed while initializing socket and streams!");
//			return null;
//		}
//	}
//
//	private void destroySocket(Object[] socketStreams) {
//		try {
//			((Socket) socketStreams[0]).close();
//			((OutputStream) socketStreams[1]).close();
//			((InputStream) socketStreams[2]).close();
//		} catch (IOException e) {
//			System.out.println("Failed while destroying the socket and the streams!");
//		}
//	}
}
