package put.ai.games.naiveplayer;

import java.util.*;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class NaivePlayer extends Player {

    public static void main(String[] args) {
    }

    private Random random = new Random(0xdeadbeef);

    @Override
    public String getName() {
        return "ZR OM";
    }

    int rozmiarPlanszy;

    int odlegloscMiedzyDwomaPunktami(Wspolrzedne<Integer> pierszyPunkt, Wspolrzedne<Integer> drugiPunkt) {
        int x1 = pierszyPunkt.getX();
        int x2 = drugiPunkt.getX();
        int y1 = pierszyPunkt.getY();
        int y2 = drugiPunkt.getY();
        return (int) Math.round(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
    }

    List<Wspolrzedne<Integer>> listaWspolrzednych(Board b, Player.Color color){
        List<Wspolrzedne<Integer>> wspolrzednePunktow = new ArrayList<Wspolrzedne<Integer>>();
        // i -> wiersz j -> kolumna
        for(int i = 0; i < rozmiarPlanszy; i++)
            for(int j = 0; j < rozmiarPlanszy; j++){
                if(color.equals(b.getState(j,i))) { //lub Color
                    wspolrzednePunktow.add(new Wspolrzedne<>(j, i));
                }
            }

        return wspolrzednePunktow;
    }


    int[] znajdzSasiadow(Wspolrzedne<Integer> obecny, int[] odwiedzone, List<Wspolrzedne<Integer>> wspolrzednePunktow){
        for (int j = wspolrzednePunktow.indexOf(obecny); j < wspolrzednePunktow.size(); j++){
            if (!wspolrzednePunktow.get(j).equals(obecny) && odwiedzone[j] == 0) {

                if (odlegloscMiedzyDwomaPunktami(wspolrzednePunktow.get(j), obecny) < 2) {
                    if(j == wspolrzednePunktow.size()) return odwiedzone;
                    odwiedzone = znajdzSasiadow(wspolrzednePunktow.get(j), odwiedzone, wspolrzednePunktow);
                    odwiedzone[j] =1;
                }
            }
        }
        return odwiedzone;
    }

    Integer grupyPionkow(List<Wspolrzedne<Integer>> wspolrzednePunktow) {

        int[] odwiedzone = new int[wspolrzednePunktow.size()];
        Arrays.fill(odwiedzone, 0);
        int grupy = 0;
        for(int i = 0; i < wspolrzednePunktow.size(); i++){
            if(odwiedzone[i] == 0) {
                grupy++;
                odwiedzone = znajdzSasiadow(wspolrzednePunktow.get(i), odwiedzone, wspolrzednePunktow);
            }
        }
        return grupy;
    }

    public class Wspolrzedne<T> {
        private T x;
        private T y;

        public Wspolrzedne(T x, T y) {
            this.x = x;
            this.y = y;
        }

        public T getX() {
            return this.x;
        }

        public T getY() {
            return this.y;
        }
    }


    int ocenaRuchu(Board b, Color color) {
        List<Wspolrzedne<Integer>> wspolrzednePunktow = listaWspolrzednych(b, getColor());
        int wynik = 0;
        int iloscGrup = grupyPionkow(wspolrzednePunktow);
        int ruchyPrzeciwnika = b.getMovesFor(getOpponent(color)).size();
        int pionkiPrzeciwnika = listaWspolrzednych(b, Player.getOpponent(getColor())).size();
        int mojePionki = listaWspolrzednych(b, getColor()).size();
        if (iloscGrup == 1) {
            wynik = Integer.MIN_VALUE;
        }

        for (Wspolrzedne<Integer> punktA: wspolrzednePunktow) {
            for (Wspolrzedne<Integer> punktB: wspolrzednePunktow) {
                if(!punktA.equals(punktB)){
                    int odleglosc = odlegloscMiedzyDwomaPunktami(punktA, punktB);
                    if(odlegloscMiedzyDwomaPunktami(punktA, punktB) >= 2){ // nie sa jeszcze polaczone -> karne punkty
                        wynik += 10 ;
                    }
                    if (odleglosc < 2) wynik -= 5;
                }
            }
            wynik  = wynik + 2 * ruchyPrzeciwnika;
            wynik += 25 * iloscGrup + 2 * ruchyPrzeciwnika;;
            wynik = wynik + (mojePionki - pionkiPrzeciwnika) * 15;
        }
        return wynik;
    }

    public Move wybierzRuch(Board b, Color color) {
        List<Move> moves = b.getMovesFor(color);
        Move obecnyRuch;
        Move wybranyRuch = null;
        int tempWynik = 0;
        int najlepszyWynik = Integer.MAX_VALUE;
        for (Move move : moves) {

            Board newBoard = b.clone();
            newBoard.doMove(move);
            obecnyRuch = move;
            long executionTime = System.currentTimeMillis() - millisActualTime;
            if(getTime() - executionTime <= 200) return wybranyRuch;
            tempWynik = alfaBeta(newBoard, 3, Integer.MIN_VALUE, Integer.MAX_VALUE,false, color);
            if (tempWynik < najlepszyWynik) {
                najlepszyWynik = tempWynik;
                wybranyRuch = obecnyRuch;
                 executionTime = System.currentTimeMillis() - millisActualTime;
                if(getTime() - executionTime <= 200) return wybranyRuch;
                
            }
        }
       
        return wybranyRuch;
    }

    public class dostepneRuchy {
        public List<Move> ruchy;

        public dostepneRuchy(Board board, Player.Color color){
            this.ruchy = board.getMovesFor(color);
        }

        public Move pobierz(){
            if(ruchy.size() <= 0) return null;
            else {
                Move move = ruchy.get(0);
                this.ruchy.remove(move);
                return move;
            }
        }

    }

    int alfaBeta(Board b, Integer depth, Integer alpha, Integer beta, Boolean maksymalizowanie, Player.Color color){

        if(depth == 0)
            return ocenaRuchu(b, color);
        dostepneRuchy pobierzRuch = new dostepneRuchy(b, color);
        //max
        if(maksymalizowanie){
            pobierzRuch = new dostepneRuchy(b, Player.getOpponent(color));
            Move move;
            Integer najlepszyWynik = Integer.MIN_VALUE;
            while((move = pobierzRuch.pobierz()) != null){
                Board kopia = b.clone();
                kopia.doMove(move);
                int wynik = alfaBeta(kopia, depth - 1, alpha, beta, false, color);
                najlepszyWynik = Math.max(wynik, najlepszyWynik);
                alpha = Math.max(wynik, alpha);
                if(beta <= alpha)
                    break;
            }
            return najlepszyWynik;
        }
        else{
            //min
            Move move;
            Integer miniWynik = Integer.MAX_VALUE;
            while((move = pobierzRuch.pobierz()) != null){
                Board kopia = b.clone();
                kopia.doMove(move);
                int wynik = alfaBeta(kopia, depth - 1, alpha, beta, true, color);
                miniWynik = Math.min(wynik, miniWynik);
                beta = Math.min(beta, wynik);
                if(beta < alpha)
                    break;
            }
            return miniWynik;
        }
    }

    long millisActualTime; // początkowy czas w milisekundach.


    @Override
    public Move nextMove(Board b) {

        millisActualTime = System.currentTimeMillis(); 
        rozmiarPlanszy = b.getSize();
        
        return wybierzRuch(b, getColor());

    }
}