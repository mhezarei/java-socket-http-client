import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class HTTPResponse {
	private final ArrayList<String> stringBody;
	private final ArrayList<Byte> byteBody;
	private final ArrayList<String> stringHead;
	private final ArrayList<Byte> fullResponse;
	private int statusCode;
	private String contentType;
	private final String path;
	private final String hostName;
	private final String fileName;
	private final ArrayList<Resource> resources;
	public static final int BUFFER_SIZE = 1;
	public int id = -1;
	
	public HTTPResponse(String path, String hostName) {
		this.path = path;
		this.hostName = hostName;
		this.fileName = HTTPClient.getFileNameFromPath(path);
		byteBody = new ArrayList<>();
		fullResponse = new ArrayList<>();
		resources = new ArrayList<>();
		stringBody = new ArrayList<>();
		stringHead = new ArrayList<>();
		contentType = "";
	}
	
	public HTTPResponse(String path, String hostName, int id) {
		this.id = id;
		this.path = path;
		this.hostName = hostName;
		this.fileName = HTTPClient.getFileNameFromPath(path);
		byteBody = new ArrayList<>();
		fullResponse = new ArrayList<>();
		resources = new ArrayList<>();
		stringBody = new ArrayList<>();
		stringHead = new ArrayList<>();
		contentType = "";
	}
	
	public void receive(InputStream inputStream) throws IOException {
		byte[] bytes = new byte[BUFFER_SIZE];
		while (inputStream.read(bytes) != -1) {
			for (byte b : bytes) {
				fullResponse.add(b);
			}
			bytes = new byte[BUFFER_SIZE];
		}
		
		int start = retrieveHead();
		System.out.println(String.join("\n", stringHead));
		extractHeaders();
		
		retrieveBody(start);
	}
	
	private void retrieveBody(int start) {
		for (int i = start; i < fullResponse.size(); i++) {
			byteBody.add(fullResponse.get(i));
		}
	}
	
	private int retrieveHead() {
		StringBuilder temp = new StringBuilder();
		for (int i = 0; i < fullResponse.size(); i++) {
			if (reachedEnd(i)) {
				return i + 4;
			} else if (isCRLF(i)) {
				stringHead.add(temp.toString());
				temp = new StringBuilder();
			} else if (fullResponse.get(i) != (byte) 10) {
				temp.append((char) fullResponse.get(i).byteValue());
			}
		}
		return -1;
	}
	
	private boolean reachedEnd(int i) {
		return fullResponse.get(i) == (byte) 13 && fullResponse.get(i + 1) == (byte) 10 &&
				fullResponse.get(i + 2) == (byte) 13 && fullResponse.get(i + 3) == (byte) 10;
	}
	
	private boolean isCRLF(int i) {
		return fullResponse.get(i) == (byte) 13 && fullResponse.get(i + 1) == (byte) 10;
	}
	
	private void extractHeaders() {
		for (String line : stringHead) {
			if (line.contains("HTTP/")) {
				statusCode = Integer.parseInt(line.split(" ")[1]);
			} else if (line.contains("Content-Type")) {
				contentType = line.split(":")[1].split(";")[0].replace(" ", "");
			}
		}
	}
	
	public ArrayList<Resource> parseResponse() {
		handleStatusCodes();
		
		handleContentType();
		
		try {
			if (contentType.contains("html")) {
				extractExternalResources(path, hostName);
			}
		} catch (IOException e) {
			System.out.println("Failed while parsing input file!");
		}
		
		return resources;
	}
	
	private void handleStatusCodes() {
		System.out.println();
		if (statusCode == 200) {
			System.out.println("200: Request was OK!");
		} else if (statusCode == 404) {
			System.out.println("404: Requested page not found!");
		} else if (statusCode == 403) {
			System.out.println("403: Access is forbidden to the requested page!");
		} else if (statusCode == 500) {
			System.out.println("500: Internal Server Error!");
		}
		System.out.println();
	}
	
	private void handleContentType() {
		switch (contentType) {
			case "image/svg+xml":
			case "application/octet-stream":
			case "text/css":
			case "text/html":
			case "application/json":
				makeStringBody();
				writeToFile();
				break;
			case "text/plain":
				makeStringBody();
				printPlainText();
				break;
			case "image/jpeg":
			case "video/x-matroska":
				writeBinaryToFile();
				break;
			default:
				break;
		}
	}
	
	private void printPlainText() {
		for (String line : stringBody) {
			System.out.print(line);
		}
		System.out.println();
	}
	
	private void makeStringBody() {
		byteBody.removeAll(Collections.singleton((byte) 0));
		byte[] bytes = new byte[byteBody.size()];
		for (int i = 0; i < byteBody.size(); i++) {
			bytes[i] = byteBody.get(i);
		}
		String str = new String(bytes, StandardCharsets.UTF_8);
		stringBody.add(str);
	}
	
	private void writeBinaryToFile() {
		FileOutputStream fileOutputStream = null;
		String fileName = getFileName();
		try {
			fileOutputStream = new FileOutputStream(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		assert fileOutputStream != null;
		byte[] result = new byte[byteBody.size()];
		for(int i = 0; i < byteBody.size(); i++) {
			result[i] = byteBody.get(i);
		}
		try {
			fileOutputStream.write(result);
		} catch (IOException e) {
			System.out.println("Failed while writing binary data to file!");
		}
	}
	
	private void writeToFile() {
		PrintWriter out;
		
		String fileName = getFileName();
		try {
			out = new PrintWriter(fileName, StandardCharsets.UTF_8);
			for (String line : stringBody) {
				out.print(line);
			}
			
			out.close();
		} catch (IOException e) {
			System.out.println("Failed while opening the output file!");
		}
	}
	
	private String getFullDirFromPath(String path) {
		String dirString = "Files" + HTTPClient.getCurrentDirFromPath(path);
		File dir = new File(dirString);
		dir.mkdirs();
		return dirString;
	}
	
	private String getFileExtFromContentType(String contentType) {
		switch (contentType) {
			case "text/html":
			case "application/json":
			case "text/css":
				return contentType.split("/")[1];
			case "image/svg+xml":
				return "svg";
			case "image/jpeg":
				return "jpg";
			case "video/x-matroska":
				return "mkv";
			case "application/octet-stream":
			default:
				// whatever the extension was
				return this.fileName.substring(this.fileName.lastIndexOf('.') + 1);
		}
	}
	
	private void extractExternalResources(String path, String hostName) throws IOException {
		File input = new File("Files/" + HTTPClient.getFileNameFromPath(path));
		Document doc = Jsoup.parse(input, "UTF-8", hostName);
		
		/*
		 * URLs are: absolute, /dir/file, ./file, ../file
		 * What to extract:
		 * `href` attr in all the elements (NOT HTML PAGES)
		 * `src` in <script>
		 * `data-background` attr in <div>
		 * `data-src` and `src` attr in <img>
		 * url is `style` attr in <div> (and other tags)
		 */
		// TODO handle html href by using HEAD request
		
		Elements links = doc.select("[href]");
		for (Element link : links) {
			String url = link.attr("href");
			addResource(url, path, hostName);
		}
		
		links = doc.select("script");
		for (Element link : links) {
			String url = link.attr("src");
			addResource(url, path, hostName);
		}
		
		links = doc.select("[data-background]");
		for (Element link : links) {
			String url = link.attr("data-background");
			addResource(url, path, hostName);
		}
		
		links = doc.select("img");
		for (Element link : links) {
			String url = link.attr("data-src");
			addResource(url, path, hostName);
			url = link.attr("src");
			addResource(url, path, hostName);
		}
		
		links = doc.select("[style]");
		for (Element link : links) {
			String styleText = link.attr("style");
			ArrayList<String> urls = extractURLFromStyleText(styleText);
			for (String url : urls) {
				addResource(url, path, hostName);
			}
		}
	}
	
	private ArrayList<String> extractURLFromStyleText(String styleText) {
		ArrayList<String> urls = new ArrayList<>();
		String[] parts = styleText.split(";", -1);
		for (String part : parts) {
			if (part.contains("url")) {
				String url = part.substring(part.indexOf("(") + 1, part.indexOf(")"));
				if (url.charAt(0) == '\'' || url.charAt(0) == '\"') {
					url = url.substring(1, url.length() - 1);
				}
				urls.add(url);
			}
		}
		return urls;
	}
	
	private void addResource(String url, String path, String hostName) {
		if (HTTPClient.isAbsoluteURL(url) && !url.startsWith("https://")) {
			resources.add(new Resource(url));
		} else if (url.startsWith("./")) {
			String dir = HTTPClient.getCurrentDirFromPath(path);
			String newPath = dir + url.substring(2);
			resources.add(new Resource("http://", newPath, hostName));
		} else if (url.startsWith("../")) {
			String dir = HTTPClient.getCurrentDirFromPath(path);
			int numOfBacks = url.split("\\.\\./", -1).length - 1;
			int dirLevels = dir.split("/", -1).length - 1;
			String newPath;
			if (numOfBacks >= dirLevels) {
				newPath = "/";
			} else {
				String[] dirSplit = dir.split("/", -1);
				newPath = String.join("/",
						Arrays.asList(dirSplit).subList(0, dirSplit.length - numOfBacks - 1)) + "/";
			}
			resources.add(new Resource("http://", newPath, hostName));
		} else if (url.startsWith("/")) {
			resources.add(new Resource("http://", url, hostName));
		}
	}
	
	private String getFileName() {
		var parts = this.fileName.split("\\.", -1);
		if (parts.length > 1) {
			parts[parts.length - 1] = "";
		}
		if (statusCode == 206 && id != -1) {
			parts[parts.length - 2] = id + "_" + parts[parts.length - 2];
		}
		
		return getFullDirFromPath(path) +
				String.join("", parts) +
				"." + getFileExtFromContentType(contentType);
	}
}
