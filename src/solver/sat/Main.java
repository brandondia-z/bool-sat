package solver.sat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Usage example: read a given cnf instance file to create 
 * a simple sat instance object and print out its parameter fields. 
 */
public class Main
{  
  public static void main(String[] args) throws Exception
  {
		if(args.length == 0)
		{
			System.out.println("Usage: java Main <cnf file>");
			return;
		}
		
		String input = args[0];
		Path path = Paths.get(input);
		String filename = path.getFileName().toString();
    
    Timer watch = new Timer();
    watch.start();
    
		SATInstance instance = DimacsParser.parseCNFFile(input);
		Map<Integer, Boolean> assignments = new HashMap<>();
		Result result = DPLL.solveSAT(instance, assignments);
	  	watch.stop();



		  StringBuilder assignmentsParsed = new StringBuilder();
	  for (int i = 0; i < instance.numVars; i++) {
		  if (assignments.containsKey(i + 1)) {
			  assignmentsParsed.append((i + 1) + " " + (assignments.get(i + 1)) + " ");
		  } else {
			  assignmentsParsed.append((i + 1) + " " + "true ");
		  }
	  }
	  System.out.println(assignmentsParsed);
		  if (result.getResult()) {
			  System.out.println("{\"Instance\": \"" + filename + "\", \"Time\": " + String.format("%.2f",watch.getTime()) + ", \"Result\": SAT" + ", \"Solution\": " + result.getAssignments() + "}");
		  } else {
			  System.out.println("{\"Instance\": \"" + filename + "\", \"Time\": " + String.format("%.2f",watch.getTime()) + ", \"Result\": UNSAT" +"}");

		  }
  }
}
