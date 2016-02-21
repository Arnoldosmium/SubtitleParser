import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.io.*;
import java.util.regex.*;
import java.util.HashMap;

public class ScriptProcessor {

	public static void main(String[] args) throws IOException{
		String prev = "\\[£Û¡¾\\(£¨";
		String aft = "¡¿£Ý£©\\)\\]";
		Pattern takeout = Pattern.compile(String.format("[%1$s][^%1$s%2$s]+[%2$s]",prev,aft));
		String spaces = "[ ¡¡\t]*";
		String pureSpaces = "^"+spaces+"$";
	
		//Load File
		Scanner sc = new Scanner(System.in);
		String configFile = "";
		try{
			sc = new Scanner(new File( configFile = (args.length == 0)? sc.next() : args[0] ));
		}catch(Exception e){
			System.err.println(e.toString());
			e.printStackTrace();
			System.err.flush();
			System.exit(1);
		}
		Scanner conf = new Scanner( new File(configFile+".config") );
		
		String nameSplitter = "£º:";
		char outputSplitter = 0;
		Pattern splitPattern = Pattern.compile(nameSplitter);
		int N = -1;
		
		try{
			nameSplitter = conf.nextLine();
		}catch(Exception e){
			System.err.println(e);
			System.out.println("Using Default: "+nameSplitter);
		}
		
		try{
			outputSplitter = conf.nextLine().charAt(0);
		}catch(Exception e){
			System.err.println(e);
			System.out.println("Using Default: null char");
		}
		
		try{
			N = Integer.parseInt(conf.nextLine());
		}catch(Exception e){
			System.err.println(e);
			conf.close();
			sc.close();
		}
		
		String[] fileNames = new String[N];
		for(int i = 0; i < N; i++){
			try{
				fileNames[i] = conf.nextLine();
			}catch(Exception e){
				System.err.println(e);
				System.out.println("Using defualt: "+ (fileNames[i] = "out"+i+".txt"));
			}
		}
		
		/* Casting Names */
		String[][] names = new String[N][];
		String[] emptyStrings = {"", ""};
		HashMap<String, String[]> nameMap = new HashMap<>();
		try{
			Pattern colon = Pattern.compile(":");
			Pattern semiC = Pattern.compile(";");
			for(int i = 0; i < N; i++){
				String ln = conf.nextLine();
				if(!ln.matches("[0-9]+"))
					throw new InputMismatchException("Expecting numbers: "+ln+"@N="+N);
				int subLen = Integer.parseInt(ln);
				names[i] = new String[subLen];
				for(int j = 0; j < subLen; j++){
					String[] tokens = colon.split(conf.nextLine());
					String[] subNames;
					if(tokens.length < 2){
						System.err.println("Ill format: "+tokens[0]);
						System.out.println("Append with itself.");
						subNames = new String[2];
						subNames[0] = tokens[0];
						subNames[1] = "";
					}else{
						subNames = semiC.split(tokens[1]);
						if(subNames.length < 2){
							System.err.println("Missing short ver: "+tokens[1]);
							System.out.println("Default: Fill with empty string");
							String[] buf = {(subNames.length == 1)? subNames[0] : "", ""};
							subNames = buf;
						}
					}
					nameMap.put(names[i][j] = tokens[0], subNames);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			System.err.flush();
			System.out.println("Processes Aborted.");
			System.exit(1);
		}

		int maxSize = 50;
		if(conf.hasNextInt())
			maxSize = conf.nextInt();
		
		/* Finish Name Capture; open pws */
		PrintWriter[] pw = new PrintWriter[N];
		try{
			for(int i = 0; i < N; pw[i] = new PrintWriter(fileNames[i++]));
		}catch(Exception e){
			System.err.println("Creating PrintWriter Error:\n"+e.toString());
			e.printStackTrace();
			System.err.flush();
			System.out.println("Processes Aborted.");
			System.exit(1);
		}

		
		/* Trim off bad stuff */
		ArrayList<String> lines = new ArrayList<String>();
		while(sc.hasNextLine()){
			String[] spl = takeout.split( sc.nextLine());
			StringBuilder sb = new StringBuilder();
			int count = 0;
			for(String s : spl)
				if(!Pattern.matches(pureSpaces, s)){
					sb.append(s);
					count++;
				}
			if(count > 0)
				lines.add(sb.toString());
		}
		
		//Constructing Patterns and Matchers
		Pattern[] validNewLine = new Pattern[pw.length];
		for(int k = 0; k < pw.length; k++){
			StringBuilder nameRegex = new StringBuilder("^"+spaces+"(");
			for(String n : names[k]){
				nameRegex.append( n+"|" );
			}
			nameRegex.setCharAt(nameRegex.length()-1 , ')');
			Pattern namePattern = Pattern.compile( nameRegex.toString() );
			nameRegex.append(spaces+"["+nameSplitter+"](.+)");
			validNewLine[k] = Pattern.compile( nameRegex.toString() );
			System.out.println(nameRegex);
		}
		
		/* Get namers */
		String currentChar = "";
		StringBuilder lineBuilder = new StringBuilder();
		String puncs = ",.:;?!£¬¡££º£»£¿£¡¡ª";
	
		Matcher[] ms = new Matcher[pw.length];
		PrintWriter currPw = pw[0];
		
		Pattern actIdentifier = Pattern.compile(spaces + "(ACT[0-9] .*$)");
		
		for(String s : lines){
			Matcher actM = actIdentifier.matcher(s);
			if(actM.matches()){
				for(PrintWriter aPw : pw){
					aPw.println(actM.group(1));
				}
				continue;
			}
			
			String[] subNames = nameMap.get(currentChar);
			if(subNames == null)
				subNames = emptyStrings;
			
			int[] maxPureScriptLine = new int[2];
			Pattern[] linePareser = new Pattern[2];
			Pattern[] lineParserBackup = new Pattern[2];
			for(int k = 0; k < 2; k++){
				maxPureScriptLine[k] = maxSize - subNames[k].length() - 2;
				linePareser[k] = Pattern.compile(String.format("%4$s(.{%2$d,%1$d})([%3$s])%4$s(.*)", maxPureScriptLine[k], maxPureScriptLine[k]/2, puncs, spaces));
				lineParserBackup[k] = Pattern.compile(String.format("(.{%2$d,%1$d})%3$s+(.*)", maxPureScriptLine[k]+1, maxPureScriptLine[k]/2 ,spaces.substring(0,spaces.length()-1)));
			}

			int whichMatch = -1;
			for(int k = 0; k < ms.length; k++){
				ms[k] = validNewLine[k].matcher(s);
				if(ms[k].matches()){
					whichMatch = k;
					break;
				}
			}
			
			if(whichMatch < 0){
				lineBuilder.append(s);
				continue;
			}
			
			int useWhich = 0;
			while(lineBuilder.length() > 0){
				char c = lineBuilder.charAt(0); 
				if(c >= 'a' && c <= 'z' )
					lineBuilder.setCharAt(0 , (char)(c - 32) );
				
				Matcher lpMch = linePareser[useWhich].matcher(lineBuilder);
				if(lpMch.matches()){
					currPw.println(subNames[useWhich] + outputSplitter + lpMch.group(1) + lpMch.group(2));
					lineBuilder = new StringBuilder(lpMch.group(3));
					continue;
				}
				lpMch = lineParserBackup[useWhich].matcher(lineBuilder);
				if(lpMch.matches()){
					//System.out.println(lineParserBackup);
					//System.out.println(lineBuilder.toString());
					currPw.println(subNames[useWhich] + outputSplitter + lpMch.group(1) );
					lineBuilder = new StringBuilder(lpMch.group(2));
					//System.out.println(lpMch.group(2));
				}else{
					currPw.println(subNames[useWhich] + outputSplitter + lineBuilder.toString());
					break;
				}
				if(useWhich == 0)	useWhich = 1;
			}
			currPw.flush();
			
			Matcher m = ms[whichMatch];
			currPw = pw[whichMatch];
			lineBuilder = new StringBuilder(m.group(2));
			currentChar = m.group(1);
		}
		
		for(int k = 0; k < pw.length; pw[k++].close());
		sc.close();
		System.out.println("Done");
	}
}
