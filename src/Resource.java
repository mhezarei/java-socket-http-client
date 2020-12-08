import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resource {
	private final String scheme;
	private final String hostName;
	private final String path;
	private final String fileName;
	
	public Resource(String url) {
		Pattern pattern = Pattern.compile(HTTPClient.ADDRESS_SCHEMA);
		Matcher matcher = pattern.matcher(url);
		matcher.find();
		this.scheme = matcher.group(1);
		this.hostName = matcher.group(2);
		this.path = matcher.group(3);
		this.fileName = HTTPClient.getFileNameFromPath(path);
	}
	
	public Resource(String scheme, String path, String hostName) {
		this.scheme = scheme;
		this.path = path;
		this.hostName = hostName;
		this.fileName = HTTPClient.getFileNameFromPath(path);
	}
	
	public String getScheme() {
		return scheme;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public String getPath() {
		return path;
	}
	
	@Override
	public String toString() {
		return "Resource{" +
				"scheme='" + scheme + '\'' +
				", hostName='" + hostName + '\'' +
				", path='" + path + '\'' +
				", fileName='" + fileName + '\'' +
				'}';
	}
}
