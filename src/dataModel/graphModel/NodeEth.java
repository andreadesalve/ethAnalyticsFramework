/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataModel.graphModel;

import java.util.Objects;

/**
 * Generic Ethereum node
 * @author andreads
 */
public class NodeEth {

    public String address ;
    //True se l'indirizzo e' un contratto false altrimenti
    public boolean isContract;

    public void setIsContract(boolean isContract) {
        this.isContract = isContract;
    }

    public boolean isContract() {
        return isContract;
    }
    
    public NodeEth(String adr){
        this.address=adr;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o) {
            return true;
        }
        // null check
        if (o == null) {
            return false;
        }
        // type check and cast
        if (getClass() != o.getClass()) {
            return false;
        }
        NodeEth c2 = (NodeEth) o;
        // field comparison
        return Objects.equals(address, c2.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
    
    public String toString(){
        return address;
    }
}
