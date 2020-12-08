# Java HTTP Client via Socket Programming

This project is an implementation of HTTP 1.0 client via socket programming in java which is 
the first assignment of the Fall 2020 Internet Engineering course.

## Description
By running the program, a CLI application will get up and running waiting for input.

**Step 1:** Program waits for the target address. The given address must match the given regex or handled in any other way.

**Step 2:** Program waits for the full request. 
The implemented methods are `GET`, `POST`, `PUT`, `PATCH`, and `DELETE`.

After receiving the response, program should make the corresponding action based on the resource type:
- **HTML document:** Save the content (i.e. response body) as a file of disk with a proper _name_ and _.html_ extension.
- **Plain text:** Simply show the response body.
- **JSON document:** Save the content (i.e. response body) as a file of disk with a proper _name_ and _.json_ extension.
- If the server responds with any HTTP status code other than 200, the program should somehow tell the user.

**Note:** Other cases can be handled however we want.

**Bonus Points:**
- **20%:** Downloading any resource type linked to the HTML like CSS stylesheets, JS scripts, Images, Fonts, etc.
**Note:** Other HTML hrefs are not included in these resource types as they might make an infinite loop.

- **10%:** Downloading any BLOB (Binary Large OBjects) like mp4 or mp3 files via multiple HTTP streams
(like the way that a download manager works).

**Note:** The program should be able to receive multiple addresses during a run, 
and should act accordingly based on the http address provided by the user, and the server response.

In addition to the steps mentioned above, the program can receive these three commands:
- `exit` command: Terminates the program.
- `set-student-id-header` command: After running this command, the CLI waits for an input from the user, 
which is the student id, and then sets this value as `x-student-id` header of every subsequent request.
- `remove-student-id-header` command: Removes the header from every subsequent request if the student id were set. 
Otherwise, it does nothing.

## Important Implementation Notes
- Request and Response both have a **Header** and a **Body**. The header of the request is where we specify what we want.
The header of the response is the meta information that the server tells us.
- Sending or receiving any kind of key-value pairs in the request header or response header which are comma-separated 
is OK, but we should use standard stuff. One of these standard stuff is `Content-Type`. 
We only need the three HTML, plain text, and JSON MIME types (and others for the bonus points).
- The `Content-Length` header holds the amounts of characters (or bytes if the request body is a BLOB) that the client
is expecting to receive in the response body. 
- If nothing comes after the hostname, client must request `"/"` resource.
- Line separators should be `"\r\n"` (CRLF) and not `"\n"` (LF).

## Testing The Program
We can send requests to the `51.89.222.172` server ip. The `x-student-id` header must be set otherwise, we will receive
a response with `401` status code.
Tests:
- `GET on "/"`: Returns a text showing whether the client is working successfully or not.
- `GET on "/test.html"`: Returns a sample HTML page which we should save.
- `GET/POST on "/sample-json"`: Returns a sample JSON format text which we should save.
- `GET on "/sample-error1"`: Returns a response with 404 status.
- `GET on "/sample-error1-with-body"`: Returns a response with 404 status with some text to show.
- `GET on "/sample-error2"`: Returns a response with 403 status.
- `GET on "/sample-error3"`: Returns a response with 500 status.
- `GET on /status`: Return the status of all requests sent to the server in **plain text**.
Might be useful for understanding JSON format and/or knowing not completed tests.
