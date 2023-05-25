/**
 * @author Borja Molina Zea
 * @version 1.0
 */

/**
 * @class SudokuHex
 * @brief Representación del sudoku que se quiere resolver
 */
public class SudokuHex {
    //Matriz de casilla
    Casilla [][] matriz = new Casilla[16][16];
    //Indica si el sudoku ha sido resuelto o no.
    public boolean fin = false;
    
    /**
     * @fn SudokuHex
     * @brief Constructor de la clase SudokuHex
     * @param template cadena de texto que representa el sudoku
     */
    public SudokuHex (String template){
	int i=0;
        //pasamos a minúsculas, para aceptar también sudokus escrito en mayus
        template = template.toLowerCase();
        //Leemos char a char y vamor rellenando las casillas
        for (int fila=0; fila<16; fila++){
            for (int col=0; col<16; col++){
                char caracterActual = template.charAt(i);
                if(caracterActual=='-'){caracterActual='.';}
                this.matriz[fila][col] = new Casilla();
                this.matriz[fila][col].valor=caracterActual;
                i++;
            }                                       
        }
        //Con los valores de las casilla, realizamos la primera actualización
        //Cada una de las casilla vacias tendrán todos los valores posibles
        //con los que que puede ser rellenada
        this.actualizarTodo();
        
    }

    /**
     * @fn solve
     * @brief función principal de la prática. Se encarga de explorar el 
     * árbol de estados asociado al sudoku, primero se elige una casilla
     * no rellena y se intenta rellenar, una vez rellenada se pasa a rellenar 
     * otra nueva, si al rellenarse con un determinado número el sudoku queda 
     * en esta irresoluble entonces da marcha atrás y quita dicho número,
     * probando otros números.
     */
    public void solve() {
        //Elegimos la casilla más prometedora
        int[] pos = this.elegirCasilla();
        int fila = pos[0];
        int col = pos[1];
        //Si no hay casilla más prometedora es porque todas están rellena
        //Damos el sudoku por resuleto y volvemos atrás
        if(fila<0 || col<0){
            this.fin = true;
            return;
        }
      
        for (int i=0; i<16; i++){
            //Una vez elegida la casilla probamos con cada uno de su valores
            //posibles
            if (this.matriz[fila][col].posibles[i]==true){
                char valorActual = Casilla.toChar(i);
                //insertamos el valor
                this.matriz[fila][col].valor=valorActual;
                this.actualizarSelectivamente(fila, col);
                //volvemos a llamar la función solve
                this.solve();
                //Si el sudoku ya esta resuelto y quedan números que probar
                //cortamos dichas pruebas, ya no nos hacen falta
                if(this.fin){return;}
            }
        }
        //Si ninguno de los número probados nos ha funcionado,
        //dejamos al casilla sin rellenar y la vuelta atrás nos hará probar
        //otro número en una casilla anteriormente rellenada
        this.quitar(fila, col);
    }
    
    /**
     * @fn toString
     * @return Una cadena de texto con el sudoku resuelto
     */
    @Override
    public String toString (){
        String resultado="";
        for (int fila=0; fila<16; fila++){
            for (int col=0; col<16; col++){
                resultado += this.matriz[fila][col].valor;
            }
            resultado += "\n";
        }
        return resultado;
    }

    /**
     * @fn InicioSector
     * @param casilla fila/columna de la que queremos averiguar la fila/columna
     * donde comienza su sector
     * @return fila/columan donde comienza el sector de casilla
     * @note Un sector es cada uno de los subcuadrados que se divide el 
     * sudoku en los cuales no se puede repetir un valor.
     */
    public int InicioSector(int casilla){
       return casilla-(casilla%4);
   }
    
    /**
     * @fn elegirCasilla
     * @brief Elige la casilla más prometedora a ser rellenada.
     * Dicha casilla será aquella que está vacía y tiene el menor número de
     * valores posibles.
     * @return array con la fila y columna de la casilla más prometedora
     */
    public int[] elegirCasilla(){
        int minimo = 17;
        int[] pos = {-1,-1};
        int tam=0;
        //Reorremos el sudoku en busca de la cailla       
        for (int fila=0; fila<16; fila++){
            for (int col=0; col<16; col++){
                //Si la casilla está vacia la tenemos en cuenta
                if (this.matriz[fila][col].valor == '.'){
                    tam = this.matriz[fila][col].numPosibles();
                    //si su número de posibles es le menor de hasta ahora
                    //la cogemos como casilla mas prometedora
                    if (tam<minimo){
                        minimo = tam;
                        pos[0] = fila;
                        pos[1] = col;
                    }
                    //si solo tiene uno posible, ya sabemos que es una casilla
                    //idonea, cortamos el bucle
                    if(tam==1){break;}
                }
            }
            //idem que el anterior break
            if(tam==1){break;}
        }
        return pos;            
    }
    
    /**
     * @gn actualizarFila
     * @brief actualiza todas las casilla vacias de una determinada fila, por
     * actualizar debe de entenderse actualizar loa posibles valores que puedan 
     * ir en dicha casilla
     * @param fila fila a ser actualizada
     */
    public void actualizarFila(int fila){
     for (int col=0; col<16; col++){
            //actualizamos solo si no tiene valor asignado
            if(this.matriz[fila][col].valor=='.'){
                this.actualizarPosibles(fila,col);
            }
            
        }
    }
     
    /**
     * @gn actualizarColumna
     * @brief actualiza todas las casilla vacias de una determinada columna, por
     * actualizar debe de entenderse actualizar loa posibles valores que puedan 
     * ir en dicha casilla
     * @param col columna a ser actualizada
     */
    public void actualizarColumna(int col){
         for (int fila=0; fila<16; fila++){
        if(this.matriz[fila][col].valor=='.'){
                this.actualizarPosibles(fila,col);
            }
        }
    }
    
    /**
     * @gn actualizarColumna
     * @brief actualiza todas las casilla vacias de un determinado sector, por
     * actualizar debe de entenderse actualizar loa posibles valores que puedan 
     * ir en dicha casilla
     * @param fila fila de uan casilla perteneciente a dicho sector
     * @param col columna de una casilla perteneciente a dicho sector
     */
    public void actualizarSector(int fila, int col){
        //hallamos el comienzo del sector que queremos actualizar
        int filaInicioSector = this.InicioSector(fila);
        int colInicioSector = this.InicioSector(col);
        int filaFinSector = filaInicioSector+4;
        int colFinSector = colInicioSector+4;
        
        for (int i=filaInicioSector; i<filaFinSector; i++){
            for(int j=colInicioSector; j<colFinSector; j++){
               if (this.matriz[i][j].valor == '.'){
                   this.actualizarPosibles(i, j);
                }
            }
        }
    }
    
    /**
     * @fn actualizarTodo
     * @brief actuliaza todas las casillas vacias del sudoku
     */
    public final void actualizarTodo(){
        for (int fila=0; fila<16; fila++){
            for (int col=0; col<16; col++){
                if(this.matriz[fila][col].valor=='.'){
                    this.actualizarPosibles(fila,col);
                }
            }
         }
    }
    
    /**
     * @fn actualizarSelectivamente
     * @brief Actualiza todas las casillas relativas a una casilla, una casilla
     * es relativa a otra si esta en la misma fila, columna o sector.
     * @param fila fila de la casilla referencia
     * @param col columna de la casilla referencia
     */
    public void actualizarSelectivamente(int fila, int col){
       this.actualizarFila(fila);        
       this.actualizarColumna(col);
       this.actualizarSector(fila, col);
    }
    
    /**
     * @fn actualizarPosibles
     * @brief actuliza los posibles valores que puede tomar una determinada 
     * casilla.
     * @param fila fila de la casilla a actualizar
     * @param col columna de la casilla a actualizar
     */
    public void actualizarPosibles(int fila, int col){
        //Para cada uno de los valores de 0 a f
        for(int pos=0; pos<16; pos++){
            char valor = Casilla.toChar(pos);
            //si el valor no esta en la fila de la casilla, ni en su columna ni
            //en su sector, entonces puede ser un posible valor de dciha 
            //casilla
            boolean resultado = this.compruebaFila(fila, valor) && this.compruebaCol(col, valor) && this.compruebaSector(fila, col, valor);
            this.matriz[fila][col].posibles[pos]=resultado;
        }
    }
    
    /**
     * @fn compruebaFila
     * @brief comprueba si un valor está ya o no en una fila
     * @param fila fila a la que se le hace la comprobación
     * @param valor valor el cual se quiere ver si está en la fila
     * @return true si el valor aún no está en la fila
     */
    public boolean compruebaFila(int fila, char valor){
        //iteramos sobre la fila
        for (int col=0; col<16; col++){
            //si el valor esta en alguna casilla del la fila ya no es posible
            //que aparezca más veces en dicha fila
            if (this.matriz[fila][col].valor == valor){
                return false;
            }
        }
        return true;
    }
    
    
   /**
     * @fn compruebaCol
     * @brief comprueba si un valor está ya o no en una columna
     * @param col columna a la que se le hace la comprobación
     * @param valor valor el cual se quiere ver si está en la columna
     * @return true si el valor aún no está en la columna
     */
    public boolean compruebaCol(int col, char valor){
        //iteramos sobre la fila
        for (int fila=0; fila<16; fila++){
            //si el valor esta en alguna casilla del la columna ya no es posible
            //que aparezca más veces en dicha columna
            if (this.matriz[fila][col].valor == valor){
                return false;
            }
        }
         return true;
    }
    
    /**
     * @fn compruebaSector
     * @param fila fila cualquiera del sector a comprobar
     * @param col columna cualquiera del sector a comprobar
     * @param valor valor que se quiere comprobar si está o no en el sector
     * @return true si el valor aún no está en el sector
     */
    public boolean compruebaSector(int fila, int col, char valor){
        //Hayamos los límites del sector de la casilla
        int filaInicioSector = this.InicioSector(fila);
        int colInicioSector = this.InicioSector(col);
        int filaFinSector = filaInicioSector+4;
        int colFinSector = colInicioSector+4;
        //iteramos sobre el sector
        for (int i=filaInicioSector; i<filaFinSector; i++){
            for(int j=colInicioSector; j<colFinSector; j++){
                //Si en alguún momento encontramos el valor,
                //ya sabemos que valor ya está en el sector
                //y que por tanto no se puede poner mas veces
                if (this.matriz[i][j].valor == valor){return false;}
            }
        }
        return true;
    }
    
    /**
     * @fn quitar
     * @brief quita el valor de una casilla y actualiza el sudoku, quitantdo 
     * las restricciones que dicho valor imponía al resto de casilla
     * @param fila fila de la casilla que se ha vuelto a poner vacia
     * @param col columna de la casilla que se ha vuelto a poner vacía
     */
    public void quitar(int fila, int col){
        this.matriz[fila][col].valor='.';
        this.actualizarSelectivamente(fila, col);
    }
       
}
