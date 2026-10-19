/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author ADM
 */
public class ComboLine {
   private int fnbItemId;
    private int quantity; 

    public ComboLine() {
    }

    public ComboLine(int fnbItemId, int quantity) {
        this.fnbItemId = fnbItemId;
        this.quantity = quantity;
    }

    public int getFnbItemId() {
        return fnbItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setFnbItemId(int fnbItemId) {
        this.fnbItemId = fnbItemId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ComboLine{" + "fnbItemId=" + fnbItemId + ", quantity=" + quantity + '}';
    }
    
}
