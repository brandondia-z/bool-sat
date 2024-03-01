package solver.sat;

import java.util.HashSet;
import java.util.Set;

public class Clause {
  private Set<Integer> literals;
  private Integer[] watchedLiterals = new Integer[2]; // Array to hold two watched literals

  public Clause(Set<Integer> literals) {
    this.literals = new HashSet<>(literals);
    // Initially, watch the first two (or fewer) literals
    int i = 0;
    for (Integer literal : literals) {
      watchedLiterals[i++] = literal;
      if (i >= 2) break;
    }
  }

  // Deep copy constructor
  public Clause(Clause other) {
    this.literals = new HashSet<>(other.literals);
    int i = 0;
    for (Integer literal : literals) {
      watchedLiterals[i++] = literal;
      if (i >= 2) break;
    }
  }

  public Set<Integer> getLiterals() {
    return literals;
  }

  public Integer[] getWatchedLiterals() {
    return watchedLiterals;
  }

  public void updateWatchedLiteral(int index, Integer newWatchedLiteral) {
    watchedLiterals[index] = newWatchedLiteral;
  }

  public void removeLiteral(Integer literal) {
    literals.remove(literal);
  }

  public boolean isEmpty() {
    return literals.isEmpty();
  }

  public boolean contains(Integer literal) {
    return literals.contains(literal);
  }
}
