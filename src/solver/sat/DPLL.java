package solver.sat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DPLL {

  public static boolean solveSAT(SATInstance instance) {
    // Initialize assignments map with no assignments
    Map<Integer, Boolean> assignments = new HashMap<>();
    if (!propagateUnitClauses(instance, assignments)) {
      return false; // UNSAT
    }
    return dpll(instance, assignments);
  }

  private static boolean dpll(SATInstance instance, Map<Integer, Boolean> assignments) {
    if (instance.getClauses().isEmpty() || allClausesSatisfied(instance, assignments)) {
      System.out.println("assignments: " + assignments);
      return true; // All clauses are satisfied
    }

    Integer variable = chooseVariable(instance, assignments);
    if (variable == null) {
      return allClausesSatisfied(instance, assignments);
    }
    List<Boolean> valuesToTry = List.of(true, false);

    for (Boolean value : valuesToTry) {
      SATInstance instanceCopy = new SATInstance(instance);
      Map<Integer, Boolean> assignmentsCopy = new HashMap<>(assignments);
//      assignmentsCopy = deepCopyMap(assignments);
      assignVariable(variable, value, instanceCopy, assignmentsCopy);
      if (propagateAssignments(instanceCopy, variable, assignmentsCopy)) {
        if (propagateUnitClauses(instanceCopy, assignmentsCopy)) {
          if (dpll(instanceCopy, assignmentsCopy)) {
              return true;
          }
        }
      }
    }
    return false; // No solution found along this path
  }

  // VARIABLE FREQUENCY HEURISTIC
  private static Integer chooseVariable(SATInstance instance, Map<Integer, Boolean> assignments) {
    // Calculate the frequency of each variable based on the variableToClauses map
    // Filter out already assigned variables
    return instance.getVars().stream()
        .filter(variable -> !assignments.containsKey(variable)) // Filter out assigned variables
        .max(Comparator.comparingInt(variable ->
            instance.getVariableToClauses().getOrDefault(variable, List.of()).size()))
        .orElse(null); // Return null only if all variables are assigned
  }

  private static void assignVariable(Integer variable, Boolean value, SATInstance instance, Map<Integer, Boolean> assignments) {
    assignments.put(variable, value);
    List<Clause> clausesWithVar = new ArrayList<>(instance.getClausesContaining(Math.abs(variable)));
    for (Clause clause : clausesWithVar) {
      if ((value && clause.contains(variable)) || (!value && clause.contains(-variable))) {
        instance.removeClause(clause);
      }
    }
  }

  private static boolean propagateAssignments(SATInstance instance, Integer variable, Map<Integer, Boolean> assignments) {
    // Propagate the assignment across the CNF, updating watched literals and checking satisfiability
    Set<Clause> affectedClauses = new HashSet<>(instance.getClausesContaining(variable));

    for (Clause clause : affectedClauses) {
      if (updateWatchedLiterals(clause, variable, assignments)) {
        return false; // Found an unsatisfiable clause during propagation
      }
    }
    return true; // Successfully propagated without finding unsatisfiability
  }

  private static boolean updateWatchedLiterals(Clause clause, Integer assignedVariable, Map<Integer, Boolean> assignments) {
    boolean needToCheckSatisfiability = true;
    int literalIndex = clause.isWatching(assignedVariable);
    if (literalIndex > 0) {
      // Try to find a new literal to watch.
      for (Integer literal : clause.getLiterals()) {
        if (!assignments.containsKey(Math.abs(literal)) && clause.isWatching(literal) < 0) {
          clause.updateWatchedLiteral(literalIndex, literal);
          needToCheckSatisfiability = false;
          break; // Found a new watched literal.
        }
      }
    }

    // Check satisfiability if no new watched literal was found or after updating.
    if (needToCheckSatisfiability || allLiteralsAssigned(clause, assignments)) {
      return isClauseSatisfiableWithCurrentAssignments(clause, assignments);
    }
    return true; // Clause remains potentially satisfiable without further check.
  }

  private static boolean isClauseSatisfiableWithCurrentAssignments(Clause clause, Map<Integer, Boolean> assignments) {
    return clause.getLiterals().stream().anyMatch(literal -> assignments.containsKey(Math.abs(literal)) && assignments.get(Math.abs(literal)) == (literal > 0));
  }

  private static boolean allLiteralsAssigned(Clause clause, Map<Integer, Boolean> assignments) {
    return clause.getLiterals().stream().allMatch(literal -> assignments.containsKey(Math.abs(literal)));
  }

  private static boolean propagateUnitClauses(SATInstance instance, Map<Integer, Boolean> assignments) {
    boolean progress;
    do {
      List<Clause> unitClauses = instance.getClauses().stream()
          .filter(clause -> clause.isUnit() && !assignments.containsKey(Math.abs(clause.getUnitLiteral())))
          .toList();

      for (Clause unitClause : unitClauses) {
        Integer unitLiteral = unitClause.getUnitLiteral();
        boolean value = unitLiteral > 0;
        assignments.put(Math.abs(unitLiteral), value);

        if (!propagateAssignments(instance, Math.abs(unitLiteral), assignments)) {
          return false; // Conflict found, UNSAT
        }
      }
      unitClauses = instance.getClauses().stream()
          .filter(clause -> clause.isUnit() && !assignments.containsKey(Math.abs(clause.getUnitLiteral())))
          .toList();
      progress = !unitClauses.isEmpty();
    } while (progress);

    return true; // No conflicts, continue
  }

  private static boolean allClausesSatisfied(SATInstance instance, Map<Integer, Boolean> assignments) {
    // Implement a check to see if all clauses are satisfied with the current assignments
    return instance.getClauses().stream().allMatch(clause -> isClauseSatisfiableWithCurrentAssignments(clause, assignments));
  }

  private DPLL() {}
}
