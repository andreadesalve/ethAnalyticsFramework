/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package prediction;

/**
 *Information about user predition
 * @author andreads
 */
public class UserInfo {
    public String userId;
    public boolean isSC;
    public long startTimeSlot;
    public long endTimeSlot;

    public UserInfo(String userId, boolean isSC, long startTimeSlot, long endTimeSlot) {
        this.userId = userId;
        this.isSC = isSC;
        this.startTimeSlot = startTimeSlot;
        this.endTimeSlot = endTimeSlot;
    }

    public String toString() {
        return "UserInfo{" + "userId=" + userId + ", isSC=" + isSC + ", startTimeSlot=" + startTimeSlot + ", endTimeSlot=" + endTimeSlot + '}';
    }
    
    
}
