import java.io.*;
import java.sql.Date;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ReadLogFiles {

	public static void main(String[] args) throws IOException, ParseException {
		
		String destinationFile = "C:\\Users\\mjibran\\Downloads\\Logs\\Processed\\Oct 23\\Sep-Oct2018.csv"; // Enter Destination file to store all the logs
		
		File fle = new File(destinationFile);
		if(fle.delete())
		{
			System.out.println("File Deleted");
		}
//		String folderLoc = "C:\\Users\\mjibran\\Desktop\\Org\\Prod_logs";
		String sourceFolder = "C:\\Users\\mjibran\\Desktop\\logs\\Prod\\Prod logs (1)\\";
		
		File folder = new File(sourceFolder);
		File[] listOfFiles = folder.listFiles();
		Arrays.sort(listOfFiles);
		Boolean insertHeader = true;
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		        System.out.println("Starting Processing File: " + file.getName());
		        processFiles(sourceFolder + "\\" + file.getName(), destinationFile, insertHeader);
		        insertHeader = false;
		        System.out.println("Ending Processing File: " + file.getName());
		        System.out.println("--------------------------------------------------------------------------------------");
		    }
		}
		
	    System.out.println("Finished Processing All logs.");
	}

	
	public static void processFiles(String dailyLogs, String combinedLogs, Boolean insertHeader) throws IOException, ParseException
	{
		int lineEndIndex = 0;
        String singleLog = "";
        BufferedReader reader = new BufferedReader(new FileReader(dailyLogs));
	    BufferedWriter writer = new BufferedWriter(new FileWriter(combinedLogs, true));
	    if(insertHeader)
	    	writer.append("Date \t Time \t ESTDateTime \t Module \t Type \t ErrorCode \t Source \t Group \t Summary \t Desc \n");
		// Read today logs	          
	    StringBuilder builder = new StringBuilder();
	    String currentLine = reader.readLine();
	    while (currentLine != null) {
	        builder.append(currentLine);	        
	        currentLine = reader.readLine();	        
	    }
	     reader.close();

	     while(lineEndIndex < builder.length() && lineEndIndex != -1)
	     {	    	
	    	 builder = builder.replace(0, lineEndIndex, "");
		     lineEndIndex = builder.indexOf("2018-", 1);
			     if( lineEndIndex != -1)
			     {
			    	 singleLog = builder.substring(0, lineEndIndex);		    	 
		    	 }
			     else
			     {
			    	 singleLog = builder.toString();
			     }
			    writer.append(pasrSeDocument(singleLog));
	     }
	     writer.flush();
		    writer.close();

	}

	private static String pasrSeDocument(String log) throws ParseException {
//		System.out.println(log);
		String dateTime = "", date="", time, msec="", module = "",type = "", source = "", desc = "", subString = "", line="", modifiedDesc="", grp="";
		Date cDateTime=null;
		String matchTrainingDoc = "[ETQ$APPLICATION_NAME=TRAINING&ETQ$FORM_NAME=TRAINING_CERTIFICATE&ETQ$KEY_NAME=CERTIFICATE_ID&ETQ$KEY_VALUE=";
		int index = 0, curCode=0;

			subString = log;
			index = subString.indexOf('[');
			if (index == 24)
			{
				dateTime = subString.substring(0, subString.indexOf('[')).trim();				
				subString = subString.replace(dateTime, "");				
				String dateTime2 = dateTime.substring(0, dateTime.indexOf(',')).trim();
				dateTime = dateTime.replace(',', ' ').trim();
				
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				// replace with your start date string
				java.util.Date d = df.parse(dateTime2); 
				Calendar gc = new GregorianCalendar();
				gc.setTime(d);
				gc.add(Calendar.HOUR, 2);
				java.util.Date d2 = gc.getTime();
				dateTime2 = d2.toString();
				System.out.println("Original :"+ dateTime + "\t Updated :"+ dateTime2);
				
				date = dateTime.substring(0, 10).trim();
				dateTime = dateTime.replace(date, "");
				
				time = dateTime;
				
				module = subString.substring(0, subString.indexOf("] ")+1).trim();
				subString = subString.replace(module, "").trim();
				
				subString = subString.trim();
				type = subString.substring(0, subString.indexOf(" ")).trim();
				subString = subString.replace(type, "");
				curCode = getInfoCode(type.trim());
				
				source = subString.substring(0, subString.indexOf('-')).trim();
				subString = subString.replace(source, "");
//				System.out.println("Sub:" + subString);
				
				String tempStr1= "-  BAYLIS:";
				if(subString.contains(tempStr1))
					subString = subString.replace(tempStr1, "");
				
				desc = subString.trim();
				desc = desc.replace("\t", "");
				desc = desc.replace("\n", "");
				desc = desc.replaceAll("\\s+", " ");
				subString = subString.replace(desc, "");
//				System.out.println("Desc:" + desc);
				String temp = desc; 
				if(desc.contains(matchTrainingDoc))
				{
					// 108
					
					modifiedDesc = temp.substring(0, 150);
					temp = temp.replace(modifiedDesc, "");
					String secondHalf = temp.substring(temp.indexOf(']'), temp.indexOf("ID=") + 3);
					modifiedDesc += secondHalf;
					temp = temp.replace(secondHalf, "");
					grp = "Training indexing error";
					modifiedDesc += temp.substring(temp.indexOf(" "));
//					System.out.println(modifiedDesc);
				}
				else if(desc.contains("could not be found for the DOCWORK_DOCUMENT form in the DOCWORK application") || desc.contains("DOCWORK_CHANGE_REQUEST_FOR_MULTIPLE_DOCUMENTS form in the DOCWORK application"))
				{
					modifiedDesc = temp.substring(0, temp.indexOf("ID=")+3);
					temp = temp.replace(modifiedDesc, "");
					modifiedDesc += temp.substring(temp.indexOf(" "));
					grp = "Doc not found";
//					System.out.println(modifiedDesc);
				}
				else if(desc.contains("All Attendees must have records in the Results subform"))
				{
					modifiedDesc = desc;
					grp = modifiedDesc;
				}
				else if (desc.contains("Ending session for") || desc.contains("has logged out") || desc.contains("session has timed out") || desc.contains("New session for") || desc.contains("License usage:") || desc.contains("Session End"))
				{
					
					modifiedDesc = "Session End"; grp = "Session";
				}
				else if(desc.contains("Login Failed. The maximum licensed number of concurrent users has been exceeded"))
				{
					modifiedDesc = desc;
					grp = "User Threshold";
				}
				else if(desc.contains("Insufficient access") || desc.contains("You have insufficient access to the"))
				{
					modifiedDesc = desc;
					grp = "Insufficient access";
				}
				else if(desc.contains("Invalid user name/password (password is case sensitive)") || desc.contains("User name is required") || desc.contains("Password is required") || desc.contains("Your user account is inactive") || desc.contains("Your user account is disabled") || desc.contains("Value of the [ Unique Passwords ] field must not be less than or equal to zero"))
				{
					modifiedDesc = desc;
					grp = "Username/Password";
				}
				else if(desc.contains("does not have an e-mail"))
				{
					modifiedDesc = desc;
					grp = "User Without Email";
				}
				else if(desc.contains("unsupported/disabled operation"))
				{
					modifiedDesc = desc;
					grp = "unsupported/disabled operation";
				}
				else if(desc.contains("Started task:") || desc.contains("Ended task:"))
				{
					modifiedDesc = desc.substring(desc.indexOf(':')+1).trim();
					grp = "Internal Task";
				}
				else if(desc.contains("Indexing process is started") || desc.contains("Indexing process is completed successfully"))
				{
					modifiedDesc = desc.substring(desc.indexOf('[')+1, desc.indexOf(']'));
					grp = "Indexing";
				}
				else if(desc.contains("Database Error: Unknown column"))
				{
					modifiedDesc = "Unknown column";
					grp = "Database Error";
				}
				else if(desc.contains("Database error encountered. Please check the log for details") || desc.contains("System switched automatically to Java search due to a failure") || desc.contains("Failed to modify the SQL statement for view"))
				{
					modifiedDesc = desc;
					grp = "Database Error";
				}
				else if(desc.contains("You have an error in your SQL syntax;"))
				{
					modifiedDesc = desc;
					grp = "Custom Query";
				}
				else if(desc.contains("Cannot archive the activity"))
				{
					modifiedDesc = desc.substring(desc.indexOf('.')+1);
					grp = "Archiving";
				}
				else if(desc.contains("nulljava") || desc.contains("at org.apache") || desc.contains("at com.etq") || desc.contains("Java Error") || desc.contains("at com.etq.reliance.control.commands.CommandBase.refreshOtherDocument") || desc.contains("at org.apache.catalina") || desc.contains("at org.apache.tomcat") || desc.contains("at org.apache.coyote") || desc.contains("at.service(Http.Proxy"))
				{
					modifiedDesc = desc;
					grp = "Java Error";
				}
				else if(desc.contains("System Error"))
				{
					modifiedDesc = desc;
					grp = "System Error";
				}
				else if(desc.contains("Page index out of range"))
				{
					modifiedDesc = desc;
					grp = "UI Error";
				}
				else if(desc.startsWith("Connection"))
				{
					modifiedDesc = desc;
					grp = "SQL";
				}
				else if(desc.contains("Processing of multipart/form-data request failed."))
				{
					modifiedDesc = "Processing of multipart/form-data request failed. SSL peer shut down incorrectly";
					grp = "File Upload";
				}
				else if(desc.contains("ValidationException:"))
				{
					modifiedDesc = desc;
					grp = "Validation Exception";
				}
				else if(desc.contains("Incorrect document data submitted"))
				{
					modifiedDesc = desc;
					grp = "Incorrect Data";
				}
				else if (desc.contains("already submitted"))
				{
					modifiedDesc = desc;
					grp = "Duplicate Submission";
				}
				else if (desc.contains("Did not found XRef object") || desc.contains("expected='%%EOF'"))
				{
					modifiedDesc = desc;
					grp = "PDF Parser";
				}
				else if (desc.contains("ANONYMOUS") || desc.contains("Anonymous"))
				{
					modifiedDesc = desc;
					grp = "Anonymous";
				}
				line = line.replaceAll("\t", "");
				line = line.replaceAll("\n", "");
				line = date + '\t' + time + '\t' + dateTime2 + '\t' + module + '\t' + type + '\t' + curCode + '\t' + source + '\t' + grp + '\t'+ modifiedDesc + '\t'+desc;
				
				line = line+"\n";
				
			}
			return line;
		}


	private static int getInfoCode(String type) {
		int infoError=3, infoInfo=1, infoWarn=0;
		if(type.equals("ERROR"))
			return infoError;
		else if(type.equals("INFO"))
			return infoInfo;
		else if(type.equals("WARN"))
			return infoWarn;
		else 
			return 99;
	}
		
	}
