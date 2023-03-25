import java.io.*;
import java.net.Socket;

public class TextEditor {
    static String address;
    static int port;
    static Socket socket;
    static BufferedReader inFromUser, inFromServer, userFileReader;
    static DataOutputStream outToServer;
    static String username;
    static boolean usernameAccepted, passwordAccepted;
    static int version;
    static String userInput;

    static void sendUsername(String username) throws IOException {
        outToServer.writeBytes("USER" + ' ' + username + '\r' + '\n');
        String response = inFromServer.readLine();
        if (response.contains("OK")) {
            usernameAccepted = true;
            TextEditor.username = username;
        }
        else {
            System.out.println("Invalid username.");
            handleServerShutdown();
        }
    }

    static void sendPassword(String password) throws IOException {
        outToServer.writeBytes("PASS" + ' ' + password + '\r' + '\n');
        String response = inFromServer.readLine();
        if (response.contains("OK")) {
            passwordAccepted = true;
        }
        else {
            System.out.println("Password for the user " + username + " is invalid.");
            handleServerShutdown();
        }
    }

    static void getVersion() throws IOException {
        outToServer.writeBytes("UPDT" + ' ' + -1 + '\r' + '\n');
        String response = inFromServer.readLine();
        if (response.contains("OK")) {
            version = Integer.parseInt(response.substring(3).split(" ", 2)[0]);
            while (inFromServer.ready()) {
                inFromServer.readLine();
            }
        }
        else {
            System.out.println("Something went wrong with getVersion().");
        }
    }

    static void getVersionAndFile() throws IOException {
        outToServer.writeBytes("UPDT" + ' ' + -1 + '\r' + '\n');
        String response = inFromServer.readLine();
        if (response.contains("OK")) {
            version = Integer.parseInt(response.substring(3).split(" ", 2)[0]);
            System.out.println("The version of the file is " + version + " and its' content are as follows:");
            System.out.println(response.substring(3).split(" ", 2)[1]);
            while (inFromServer.ready()) {
                System.out.println(inFromServer.readLine());
            }
        }
        else {
            System.out.println("Something went wrong with getVersionAndFile().");
        }
    }

    static void appendToFile(String text) throws IOException {
        outToServer.writeBytes("APND" + ' ' + version + ' ' + text + '\r' + '\n');
        String response = inFromServer.readLine();
        if (response.contains("OK")) {
            version = Integer.parseInt(response.substring(3));
            System.out.println("Successfully appended to the file. Updated file version: " + version);
        }
        else {
            System.out.println("Something went wrong with appendToFile(text).");
        }
    }

    static void writeToFile(int lineNumber, String text) throws IOException {
        outToServer.writeBytes("WRTE" + ' ' + version + ' ' + lineNumber + ' ' + text + '\r' + '\n');
        String response = inFromServer.readLine();
        if (response.contains("OK")) {
            version = Integer.parseInt(response.substring(3));
            System.out.println("Successfully written to the file. Updated file version: " + TextEditor.version);
        }
        else if (response.contains("No such line exists.")){
            System.out.println("Line " + lineNumber + " does not exist. Please check your input and try again.");
        }
        else {
            System.out.println("Something went wrong with writeToFile(lineNumber, text).");
        }
    }

    static void terminate() throws IOException {
        outToServer.writeBytes("EXIT" + '\r' + '\n');
        String response = inFromServer.readLine();
        if (response.contains("OK")) {
            System.out.println("Goodbye.");
            socket.close();
        }
    }

    static void appendFromUserFile(String fileName) throws IOException {
        userFileReader = new BufferedReader(new FileReader(fileName));
        while (userFileReader.ready()) {
            appendToFile(userFileReader.readLine());
        }
    }

    static void handleServerShutdown() throws IOException {
        socket.close();
        socket = new Socket(address, port);
        outToServer = new DataOutputStream(socket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        reAuthenticate();
    }

    static void reAuthenticate() throws IOException {
        if (usernameAccepted) {
            sendUsername(username);
        }
    }

    public static void main(String... args) throws IOException {
        address = args[0];
        port = Integer.parseInt(args[1]);
        socket = new Socket(address, port);
        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outToServer = new DataOutputStream(socket.getOutputStream());
        usernameAccepted = false;
        passwordAccepted = false;
        version = -1;
        userInput = "";

        while (!socket.isClosed()) {
            System.out.println("Enter your username below:");
            while (!usernameAccepted) {
                sendUsername(inFromUser.readLine());
            }
            System.out.println("Hello, " + username + ", enter your password below:");
            while (!passwordAccepted) {
                sendPassword(inFromUser.readLine());
            }
            System.out.println("User authenticated.");

            System.out.println("Here are the operations you can do:");
            System.out.println("1) Get the latest version of the file and its' contents.");
            System.out.println("2) Append some text to the end of the file.");
            System.out.println("3) Write some text to a specific line in the file.");
            System.out.println("4) Append lines in bulk from a txt file.");
            System.out.println("5) Quit TextEditor.");

            while (!userInput.equals("5")) {
                System.out.println("Please enter the corresponding number for the operation you would like to do below:");
                userInput = inFromUser.readLine();
                switch (userInput) {
                    case "1":
                        getVersionAndFile();
                        break;
                    case "2":
                        getVersion();
                        System.out.println("Please enter the text you would like to append to the file below:");
                        appendToFile(inFromUser.readLine());
                        break;
                    case "3":
                        getVersion();
                        System.out.println("Please enter the line you would like to write text to below:");
                        try {
                            int lineNumber = Integer.parseInt(inFromUser.readLine());
                            System.out.println("Please enter the text you would like to write to line " + lineNumber + " below:");
                            writeToFile(lineNumber, inFromUser.readLine());
                        } catch (NumberFormatException e) {
                            System.out.println("Unable to parse user input. Please enter a numeric value.");
                        }
                        break;
                    case "4":
                        getVersion();
                        System.out.println("Please enter the name of the file you would like to append in bulk:");
                        try {
                            appendFromUserFile(inFromUser.readLine());
                        }
                        catch (FileNotFoundException e) {
                            System.out.println("The specified file is not found. Please try again.");
                        }
                        break;
                    case "5":
                        terminate();
                        break;
                    default:
                        System.out.println("User input does not correspond with the provided operation codes. Try again.");
                        break;
                }
            }
        }

    }
}