/**
 * @author Borja Molina Zea
 * @version 1.0
 */

/**
 * @class Casilla
 * @brief Representa una casilla del sudoku
 * @param valor valor que la casilla posee
 * @param posible Array de booleanos con los posibles valores que una casilla
 * puede tomar.
 */

public class Casilla {
    public char valor;
    public boolean [] posibles;
    
    /**
     * @fn Casilla
     * @brief constructor de la clase casilla.
     */
    public Casilla (){
        this.valor='.';
        this.posibles = new boolean [16];
        for (int i=0; i<16; i++){
            this.posibles[i]= true;
        }
    }      
    
    /**
     * @fn tamPosibles
     * @return Número de posible valores que una casilla puede tomar.
     */
    public int numPosibles(){
        int tam = 0;
        for (int i=0; i<16; i++){
            if (this.posibles[i]==true){tam++;}
        }
        return tam;
    }
    
    /**
     * @fin toChar
     * @param valor entero a convertir
     * @return valor char del entero
     */
    public static char toChar(int valor){
        if(valor==0){return '0';}
        if(valor==1){return '1';}
        if(valor==2){return '2';}
        if(valor==3){return '3';}
        if(valor==4){return '4';}
        if(valor==5){return '5';}
        if(valor==6){return '6';}
        if(valor==7){return '7';}
        if(valor==8){return '8';}
        if(valor==9){return '9';}
        if(valor==10){return 'a';}
        if(valor==11){return 'b';}
        if(valor==12){return 'c';}
        if(valor==13){return 'd';}
        if(valor==14){return 'e';}
        if(valor==15){return 'f';}
        return '.';
    }
  
}
