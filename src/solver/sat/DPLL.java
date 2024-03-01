package solver.sat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DPLL {

  private static Map<Integer, Boolean> lastPhase = new HashMap<>();

  public static boolean solveSAT(SATInstance instance) {
    // Initialize assignments map with no assignments
    Map<Integer, Boolean> assignments = new HashMap<>();
    if (!initializeWatchedLiterals(instance, assignments)) {
      return false; // Initialization failed due to an unsatisfiable unit clause
    }
    return dpll(instance, assignments);
  }

  private static boolean dpll(SATInstance instance, Map<Integer, Boolean> assignments) {
    if (assignments.size() == instance.getNumVars()) {
      System.out.println("assignments: " + assignments);
      return true; // Solution found
    }

    SATInstance instanceCopy = new SATInstance(instance);
    Map<Integer, Boolean> assignmentsCopy = new HashMap<>(assignments);

    Integer variable = chooseVariable(instanceCopy, assignmentsCopy);
    List<Boolean> valuesToTry = lastPhase.containsKey(variable) ?
        List.of(lastPhase.get(variable), !lastPhase.get(variable)) :
        List.of(true, false);

    for (Boolean value : valuesToTry) {
      assignVariable(variable, value, instanceCopy, assignmentsCopy);
      if (updateWatchedLiteralsAndCheck(instanceCopy, variable, assignmentsCopy)) {
        if (dpll(instanceCopy, assignmentsCopy)) {
          return true;
        }
      }
      instanceCopy = new SATInstance(instance);
      assignmentsCopy = new HashMap<>(assignments);
    }
    return false; // No solution found along this path
  }

  // VARIABLE FREQUENCY HEURISTIC
  private static Integer chooseVariable(SATInstance instance, Map<Integer, Boolean> assignments) {
    // Calculate the frequency of each variable based on the variableToClauses map
    // Filter out already assigned variables
    Integer chosen = instance.getVars().stream()
        .filter(variable -> !assignments.containsKey(variable)) // Filter out assigned variables
        .max(Comparator.comparingInt(variable ->
            instance.getVariableToClauses().getOrDefault(variable, List.of()).size() +
                instance.getVariableToClauses().getOrDefault(-variable, List.of()).size()))
        // Combine counts for a variable and its negation
        .orElse(null); // Return null only if all variables are assigned
    if (chosen == null) System.out.println("WARNING RETURNING NULL");
    return chosen;
  }

  private static void assignVariable(Integer variable, Boolean value, SATInstance instance, Map<Integer, Boolean> assignments) {
    assignments.put(variable, value);
    lastPhase.put(variable, value);
    List<Clause> clausesWithVar = new ArrayList<>(instance.getClausesContaining(Math.abs(variable)));
    for (Clause clause : clausesWithVar) {
      if ((value && clause.contains(variable)) || (!value && clause.contains(-variable))) {
        instance.removeClause(clause);
      }
    }
  }

  private static boolean updateWatchedLiteralsAndCheck(SATInstance instance, Integer variable, Map<Integer, Boolean> assignments) {
    for (Clause clause : instance.getClausesContaining(variable)) {
      if (clause.isEmpty()) {
        return false; // Found an unsatisfiable clause, formula is UNSAT
      }
      if (!isClauseSatisfied(clause, assignments) && !canClauseBeSatisfied(clause, assignments)) {
        return false; // Found an unsatisfiable clause, formula is UNSAT
      }
      // If clause becomes a unit clause, ensure its unit literal is assigned appropriately here
      if (clause.getLiterals().size() == 1) {
        Integer literal = clause.getLiterals().iterator().next();
        boolean value = literal > 0;
        assignVariable(Math.abs(literal), value, instance, assignments);
        if (updateWatchedLiteralsAndCheckForAll(instance, assignments)) {
          return false; // Found an unsatisfiable clause, formula is UNSAT
        }
      }
    }
    return true; // No unsatisfiable clause found
  }

  private static boolean canClauseBeSatisfied(Clause clause, Map<Integer, Boolean> assignments) {
    for (Integer literal : clause.getLiterals()) {
      // Check if the literal is unassigned; if so, the clause can potentially be satisfied.
      if (!assignments.containsKey(Math.abs(literal))) {
        return true;
      }

      // If the literal is assigned, check if its assignment satisfies the clause.
      Boolean assignedValue = assignments.get(Math.abs(literal));
      boolean isLiteralPositive = literal > 0;

      // If the literal's assignment aligns with its polarity in the clause, the clause is satisfied.
      if (assignedValue == isLiteralPositive) {
        return true;
      }
    }

    // If no unassigned or satisfying assigned literal is found, the clause cannot be satisfied.
    return false;
  }

  private static boolean updateWatchedLiterals(Clause clause, Map<Integer, Boolean> assignments) {
    Integer[] watchedLiterals = clause.getWatchedLiterals();
    for (int index = 0; index < watchedLiterals.length; index++) {
      Integer watchedLiteral = watchedLiterals[index];
//      System.out.println("checking to skip watched literal");
//      System.out.println("watchedLiteral: " + Math.abs(watchedLiteral));
//      System.out.println("assignments: " + assignments);
      if (watchedLiteral == null || !assignments.containsKey(Math.abs(watchedLiteral)) || (assignments.containsKey(Math.abs(watchedLiteral)) && assignments.get(Math.abs(watchedLiteral)) == (watchedLiteral > 0))) {
//        System.out.println("skipping watched literal: " + watchedLiteral);
        continue; // This watched literal is either not set or satisfied, no update needed
      }

      // Try to find a new literal to watch
      boolean foundReplacement = false;
//      System.out.println("set of literals" + clause.getLiterals());
      for (Integer literal : clause.getLiterals()) {
//        System.out.println("checking for replacements");
//        System.out.println("assignments: " + assignments);
//        System.out.println("literal: " + literal);
        if (!assignments.containsKey(Math.abs(literal)) && (!literal.equals(watchedLiterals[1 - index]))) {
          clause.updateWatchedLiteral(index, literal);
          foundReplacement = true;
          break; // Found a replacement, break out of the loop
        }
      }

      if (!foundReplacement) {
        // No replacement found, meaning the clause could be unsatisfied
//        System.out.println("no replacement found: " + watchedLiteral);
        boolean clausePotentiallySatisfied = false;
        for (Integer literal : clause.getLiterals()) {
          if (!assignments.containsKey(Math.abs(literal)) || (assignments.containsKey(Math.abs(literal)) && assignments.get(Math.abs(literal)) == (literal > 0))) {
            clausePotentiallySatisfied = true;
            break; // Clause is satisfied by at least one literal
          }
        }
        if (!clausePotentiallySatisfied) {
//          System.out.println("clause is unsatisfied: " + watchedLiteral);
//          System.out.println("clause: " + clause.getLiterals());
          return true; // Clause is truly unsatisfied
        }
      }
    }
    return false; // Updated successfully or no update needed
  }

  private static boolean initializeWatchedLiterals(SATInstance instance, Map<Integer, Boolean> assignments) {
    for (Clause clause : instance.getClauses()) {
      // Your Clause constructor already initializes watched literals.
      // For unit clauses, immediately apply their value:
      if (clause.getLiterals().size() == 1) {
        Integer literal = clause.getLiterals().iterator().next();
        boolean value = literal > 0;
        assignments.put(Math.abs(literal), value);
        // If applying a unit clause makes any clause unsatisfied, return false
        if (updateWatchedLiteralsAndCheckForAll(instance, assignments)) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean updateWatchedLiteralsAndCheckForAll(SATInstance instance, Map<Integer, Boolean> assignments) {
    for (Clause clause : instance.getClauses()) {
      // Check if the clause is already satisfied to skip unnecessary updates
      if (isClauseSatisfied(clause, assignments)) {
        continue;
      }

      // Attempt to update the watched literals for the clause
      if (updateWatchedLiterals(clause, assignments)) {
        // If it's not possible to update the watched literals to avoid unsatisfaction, the clause is unsatisfied
        return true;
      }
    }
    return false; // All clauses are either satisfied or have been successfully updated
  }

  private static boolean isClauseSatisfied(Clause clause, Map<Integer, Boolean> assignments) {
    for (Integer watchedLiteral : clause.getWatchedLiterals()) {
      if (watchedLiteral == null) {
        continue; // Skip null watched literals
      }
      // If either of the watched literals is satisfied, the clause is satisfied
      if (assignments.containsKey(Math.abs(watchedLiteral)) && assignments.get(Math.abs(watchedLiteral)) == (watchedLiteral > 0)) {
        return true;
      }
    }
    // Additionally, check all literals in the clause if neither watched literal is clearly satisfying
    for (Integer literal : clause.getLiterals()) {
      if (assignments.containsKey(Math.abs(literal)) && assignments.get(Math.abs(literal)) == (literal > 0)) {
        return true;
      }
    }
    return false; // No satisfying literal found, clause is not satisfied yet
  }

  private DPLL() {}
}
