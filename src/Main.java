import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	public static void main(String[] args) {
		var mainScanner = new Scanner(System.in);
		var client = new HTTPClient();
		
		while (true) {
			System.out.println("Please enter your desired command or url:");
			var command = mainScanner.nextLine();
			
			commandHandler(mainScanner, client, command);
		}
	}
	
	private static void commandHandler(Scanner mainScanner, HTTPClient client, String command) {
		if (command.equals("exit")) {
			System.out.println("UNDERSTANDABLE. HAVE A GREAT DAY.");
			System.exit(0);
		} else if (command.equals("set-student-id-header")) {
			System.out.println("Please enter your student id:");
			try {
				client.setStudentId(Integer.parseInt(mainScanner.nextLine()));
				System.out.println("Student id successfully added!");
			} catch (Exception e) {
				System.out.println("Incorrect input! Please reenter the command.");
			}
		} else if (command.equals("remove-student-id-header")) {
			client.setStudentId(0);
			System.out.println("Student id successfully removed!");
		} else if (HTTPClient.isAbsoluteURL(command)) {
			Pattern pattern = Pattern.compile(HTTPClient.ADDRESS_SCHEMA);
			Matcher matcher = pattern.matcher(command);
			if (matcher.find()) {
				System.out.println("Very cool! Now please enter the method:");
				System.out.println("(Available methods are GET, POST, PUT, PATCH, and DELETE)");
				String method = mainScanner.nextLine().toUpperCase();
				if (method.equals("GET") || method.equals("PUT") ||
						method.equals("PATCH") || method.equals("DELETE") || method.equals("HEAD")) {
					client.connect(matcher, method, null);
				} else if (method.equals("POST")) {
					var queries = handleQueries(mainScanner);
					client.connect(matcher, method, queries);
				} else {
					System.out.println("This method is either wrong or not available!");
				}
			} else {
				System.out.println("The given address is not HTTP! Please try again.");
			}
		} else {
			System.out.println("Incorrect Input! Please try again.");
		}
	}
	
	private static HashMap<String, String> handleQueries(Scanner mainScanner) {
		System.out.println("Great! Now please enter you query parameters");
		System.out.println("In the 'key value' format:");
		System.out.println("(enter END when finished)");
		var queries = new HashMap<String, String>();
		while (true) {
			String line = mainScanner.nextLine();
			if (line.equals("END")) {
				return queries;
			}
			var q = line.split(" ");
			if (q.length == 2) {
				queries.put(q[0], q[1]);
			} else {
				System.out.println("Please reenter the query in the correct format!");
			}
		}
	}
}
