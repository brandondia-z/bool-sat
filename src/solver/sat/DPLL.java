package solver.sat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DPLL {
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

    Integer variable = chooseVariable(instance, assignments);
    List<Boolean> valuesToTry = List.of(true, false);

    for (Boolean value : valuesToTry) {
      if (assignVariable(variable, value, instanceCopy, assignmentsCopy)) {
        if (updateWatchedLiteralsAndCheck(instanceCopy, variable, assignmentsCopy)) {
          if (dpll(instanceCopy, assignmentsCopy)) { // Pass a copy for backtracking
            return true;
          }
        }
      }
      instanceCopy = new SATInstance(instance);
      assignmentsCopy = new HashMap<>(assignments);
    }
    return false; // No solution found along this path
  }

  private static Integer chooseVariable(SATInstance instance, Map<Integer, Boolean> assignments) {
    for (Integer var : instance.getVars()) {
      if (!assignments.containsKey(var)) {
        return var;
      }
    }
    System.out.println("WARNING RETURNING NULL");
    return null;
  }

  private static boolean assignVariable(Integer variable, Boolean value, SATInstance instance, Map<Integer, Boolean> assignments) {
    assignments.put(variable, value);
//    List<Clause> clausesWithVar = instance.getClausesContaining(variable);
    List<Clause> clausesWithVar = new ArrayList<>(instance.getClausesContaining(Math.abs(variable)));
    for (Clause clause : clausesWithVar) {
      // if a clause contains this literal, remove the entire clause
      // if a clause contains the negation of this literal, remove the literal
      if ((value && clause.contains(variable)) || (!value && clause.contains(-variable))) {
        instance.removeClause(clause);
      } else {
        if (value) {
          clause.removeLiteral(-variable);
          instance.removeFromVariableToClauses(variable, clause);
        } else {
          clause.removeLiteral(variable);
          instance.removeFromVariableToClauses(variable, clause);
        }
        if (clause.isEmpty()) {
          return false; // Found an unsatisfiable clause, formula is UNSAT
        }
      }
    }

    for (int i = 0; i < instance.getClauses().size(); i++) {
      Clause clause = instance.getClauses().get(i);
      if (clause.contains(variable)) {
        clause.removeLiteral(variable);
        instance.removeFromVariableToClauses(variable, clause);
        if (clause.isEmpty()) {
          return false; // Found an unsatisfiable clause, formula is UNSAT
        }
      }
    }


    return true;
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
        if (!updateWatchedLiteralsAndCheckForAll(instance, assignments)) {
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
        return false;
      }
    }
    return true; // All clauses are either satisfied or have been successfully updated
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
