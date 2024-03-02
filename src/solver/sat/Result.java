package solver.sat;

import java.util.Map;

public class Result {
    private boolean result;
    private Map<Integer, Boolean> assignments;
  public Result(boolean result, Map<Integer, Boolean> assignments){
    this.result = result;
    this.assignments = assignments;
  }
    public boolean getResult(){
        return result;
    }
    public Map<Integer, Boolean> getAssignments(){
        return assignments;
    }
}
