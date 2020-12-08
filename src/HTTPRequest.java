import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class HTTPRequest {
	private final ArrayList<String> request;
	private final String method;
	private final StringBuilder path;
	private final String hostName;
	private final int studentId;
	HashMap<String, String> queries;
	private long[] range;
	
	public HTTPRequest(String method, String path, String hostName, int studentId, long start, long end) {
		request = new ArrayList<>();
		this.method = method;
		this.path = new StringBuilder(path);
		this.hostName = hostName;
		this.studentId = studentId;
		range = new long[2];
		range[0] = start;
		range[1] = end;
	}
	
	public HTTPRequest(String method, String path, String hostName, int studentId, HashMap<String, String> queries) {
		request = new ArrayList<>();
		this.method = method;
		this.path = new StringBuilder(path);
		this.hostName = hostName;
		this.studentId = studentId;
		this.queries = queries;
	}
	
	public void send(OutputStream outputStream) throws IOException {
		fillRequest();
		
		for (String string : request) {
			outputStream.write(string.getBytes());
		}
		outputStream.flush();
		
		for (String string : request) {
			System.out.print(string);
		}
	}
	
	private void fillRequest() {
		if (method.equals("POST")) {
			addQueries();
		}
		
		if (isPathEnglish(path)) {
			request.add(method + " " + path.toString() + " HTTP/1.0" + HTTPClient.CRLF);
		} else {
			request.add(method + " " + encode(new StringBuilder(path)).toString() +
					" HTTP/1.0" + HTTPClient.CRLF);
		}
		
		request.add("Host: " + hostName + HTTPClient.CRLF);
		if (studentId != 0) {
			request.add("x-student-id: " + studentId + HTTPClient.CRLF);
		}
		request.add("Accept: */*" + HTTPClient.CRLF);
		request.add("Accept-Language: en-us" + HTTPClient.CRLF);
		request.add("Accept-Encoding: gzip, deflate, br" + HTTPClient.CRLF);
		if (path.toString().contains("mkv") || (range != null && range.length == 2)) {
			request.add("Range: bytes=" + range[0] + "-" + range[1] + HTTPClient.CRLF);
		}
		request.add(HTTPClient.CRLF);
	}
	
	private void addQueries() {
		if (queries != null) {
			path.append("?");
			StringBuilder q = new StringBuilder();
			for (String key : queries.keySet()) {
				q.append(key).append("=").append(queries.get(key)).append("&");
			}
			path.append(q, 0, q.length() - 1);
		}
	}
	
	private StringBuilder encode(StringBuilder path) {
		StringBuilder newPath = new StringBuilder();
		for (int i = 0; i < path.length(); i++) {
			int c = path.codePointAt(i); // the unicode of the char
			if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0xFB50 && c <= 0xFDFF) ||  (c >= 0xFE70 && c <= 0xFEFF)) {
				String s = String.valueOf((char) c);
				newPath.append(URLEncoder.encode(s, StandardCharsets.UTF_8));
			} else {
				newPath.append(path.charAt(i));
			}
		}
		return newPath;
	}
	
	private boolean isPathEnglish(StringBuilder path) {
		return Pattern.compile("^[a-zA-Z0-9?><;,{}\\[\\]\\-_+=!@#$%^&*|'/.]+$").matcher(path.toString()).find();
	}
}
