/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataModel.graphModel;

/**
 * Interazione ethereum tra due nodi
 * @author andreads
 */
public class Interaction {
    //Identificatore univoco interazione (corrisponde all'hash della transazione)

    String txsId;
    //Sorgente
    String src;
    //Destinatario
    String dst;
    //Blocco in cui si trova la transazione
    int blockNumber;
    //The value being transacted in ether
    String value;
    //The value of the fee in ether
    double txnFee;
    boolean isContractSrc;
    boolean isContractDst;
    String operation;
    /**
     * [] = Transaction
     * [n] = Call
     */
    String traceAddress = null;
    private String gasPrice;
    private String gasLimit;
    private String gasUsed;
    //private double weight = 1.0;
    private int subtraces;

    public double getTxnFee() {
        return txnFee;
    }

    public Interaction() {
    }

    public void setTraceAddress(String traceAddress) {
        this.traceAddress = traceAddress;
    }

    public String getTraceAddress() {
        return traceAddress;
    }

    public boolean isInternalTransaction() {
        return !"[]".equals(this.traceAddress);
    }

    public Interaction(String txsId) {
        this.txsId = txsId;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getTxsId() {
        return txsId;
    }

    public void setTxsId(String txsId) {
        this.txsId = txsId;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Interaction other = (Interaction) obj;
        
       
        if (this.txsId.equals(other.txsId) && this.traceAddress.equals(other.traceAddress)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.txsId.hashCode();
    }

    @Override
    public String toString() {
        //return parentHashTnx!=null? "int tnx": "tnx";
        return String.valueOf(txsId);
    }

//    public String toString2() {
//        return txsId + " - " + src.toString() + "(" + isContractSrc + ") -> " + dst.toString() + " (" + isContractDst + ") Ether " + value + " Fee: " + txnFee + " Ether";
//    }
    
      public String toString2() {
        return txsId +  "(" + isContractSrc + ") -> "  + isContractDst + ") Ether " + value + " Fee: " + txnFee + " Ether";
    }

    public String getValue() {
        return value;
    }

    public void setTxnFee(double txnFee) {
        this.txnFee = txnFee;
    }

    public void setContractSrc(boolean contract) {
        this.isContractSrc = contract;
    }

    public void setContractDst(boolean contract) {
        this.isContractDst = contract;
    }

    public boolean isContractSrc() {
        return this.isContractSrc;
    }

    public boolean isContractDst() {
        return this.isContractDst;
    }

//    public void setWeight(double d) {
//        this.weight = d;
//    }

   // public double getWeight() {
  //      return this.weight;
  //  }

    public void setGasUsed(String gasUsed) {
        this.gasUsed = gasUsed;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setOperation(String op) {
        this.operation = op;
    }

    public void setSubtraces(int subtraces) {
        this.subtraces = subtraces;
    }

    public boolean isContractCreation() {
        return this.operation.contains("create");
    }
}
