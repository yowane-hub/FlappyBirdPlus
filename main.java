// Classe représentant l'oiseau dans Flappy bird 
class Oiseau{
    // position horizontale (x) et verticale (y) de l'oiseau
    public double x;
    public double y;

    // vitesse verticale: negative=monte, positive=descend
    public double vitesseY;

    // nombres de vie restantes(3 vies par partie)
    public int vies;

    // methode pour faire sauter l'oiseau quand l'utilisateur appuie sur la touche espace
    public void sauter(){
        this.vitesseY=-8.0;
    }

    // mettre à jour la chute de l'oiseau à chaque étape
    public void mettreAjour(){
        double gravite=0.5; // la force qui attire l'oiseaux vers le bas
         this.vitesseY=this.vitesseY+gravite; //La vitesse vers le bas augmente
        this.y=this.y+this.vitesseY; //la position Y change en fonction de la vitesse
    }

    // methode pour perdre 1 vie en cas de choc
    public void perdreVie(){
        // On évite de descendre en dessous de 0 vie
        if(this.vies>0){
            this.vies = this.vies - 1;
        }
    }

    // Méthode pour afficher l'état de l'oiseau dans la console
    public void afficherPosition() {
        System.out.println("x=" + this.x + " | y=" + this.y + " | vitesseY=" + this.vitesseY + " | vies=" + this.vies);
    }
}

public class main{
    public static void main(String[] args){
        Oiseau monOiseau = new Oiseau(); //Creeation d'objet monOiseau de la classe Oiseau

        // initialisation de valeur de départ de l'oiseau
        monOiseau.x=100;
        monOiseau.y=250;
        monOiseau.vitesseY=0;
        monOiseau.vies=3;

        System.out.println("--- État initial ---");
        monOiseau.afficherPosition();

       System.out.println("--- Le joueur fait un saut (flap) ---");
        monOiseau.sauter();
        monOiseau.afficherPosition();

        System.out.println("--- Simulation de 4 pas de gravité ---");
        for (int i = 1; i <= 4; i++) {
            monOiseau.mettreAjour();
            System.out.print("Pas " + i + " : ");
            monOiseau.afficherPosition();
        }

        System.out.println("--- Simulation d'un choc (-1 vie) ---");
        monOiseau.perdreVie();
        monOiseau.afficherPosition();
    }

}