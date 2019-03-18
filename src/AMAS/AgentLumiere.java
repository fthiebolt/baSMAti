package AMAS;

import Enumerations.Constantes;
import Enumerations.Metrique;
import Physical.StateLumiere;

import java.util.concurrent.TimeUnit;

public class AgentLumiere extends AgentNeoCampus{

    private StateLumiere state;
    private StateLumiere lastState;
    private StateLumiere stateRollback;

    public AgentLumiere(AmasNeoCampus amas) {
        super(amas, Metrique.LUMIERE);
        //TODO initialiser les capteurs effecteur suivant MQTT comment c'est plus simple ( dans state )
        state = new StateLumiere();
        state.updateValues();
        lastState = new StateLumiere(state);
    }

    @Override
    protected void onPerceive() {
        // TODO ici on devra interroger l'interface MQTT pour remplir les données liées a nos capteurs

        // on sauvegarde, pour le rollback en cas de bouclage le lastState précédent
        stateRollback = new StateLumiere(lastState);

        //on update le lastState (en dernier dans la fonction)
        lastState = new StateLumiere(state);

        // On regarde l'etat des capteurs et l'etat de l'effecteur et on remplis les boolens de state ( bright tout ca )
        // suivant les valeurs brutes ( en lux ) recu de l'interface.

    }

    @Override
    protected void onDecideAndAct() {
        // TODO suivant l'etat actuel et l'etat precedent, mis en place d'une action si besoin est
        if(!(lastState.getIsOn() == state.getIsOn())) {
            // user a override l'état de l'effecteur; il faut donc dormir
            try {
                TimeUnit.MINUTES.sleep(Constantes.MINUTES_USER_OVERRIDES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            decision();
        }
    }

    //##################################################################################################################
    //fonction avec tout les cas et decision, la plus compréhensible mais sale
    // ( j'ai pris la liste des cas critiques de bas en haut )

    private void allCasesDecision() {
        if (state.getIsPresence() && state.getIsBrightOutside() && state.getIsBrightInside() && state.getIsOn()) {
            // critique economie
            // demander a l'interface d'eteindre notre lampe
        } else if (state.getIsPresence() && state.getIsBrightOutside() && !state.getIsBrightInside() && !state.getIsOn()) {
            // critique confort
            // demander a l'interface d'allumer notre lampe
        } else if (state.getIsPresence() && !state.getIsBrightOutside() && !state.getIsBrightInside() && !state.getIsOn()){
            // critique confort
            // demander a l'interface d'allumer notre lampe
        } else if (!state.getIsPresence() && state.getIsBrightOutside() && state.getIsBrightInside() && state.getIsOn()){
            // critique economie
            // demander a l'interface d'eteindre notre lampe
        } else if (!state.getIsPresence() && state.getIsBrightOutside() && !state.getIsBrightInside() && state.getIsOn()){
            // critique economie
            // demander a l'interface d'eteindre notre lampe
        } else if (!state.getIsPresence() && !state.getIsBrightOutside() && state.getIsBrightInside() && state.getIsOn()){
            // critique economie
            // demander a l'interface d'eteindre notre lampe
        } else if (!state.getIsPresence() && !state.getIsBrightOutside() && !state.getIsBrightInside() && state.getIsOn()){
            // critique economie
            // demander a l'interface d'eteindre notre lampe
        }
    }

    //##################################################################################################################
    //tout les cas concentré sur les deux actions possible, moins clair, plus court

    private boolean bigIsTurnOn(){
        return (state.getIsPresence() && state.getIsBrightOutside() && !state.getIsBrightInside() && !state.getIsOn())
        || (state.getIsPresence() && !state.getIsBrightOutside() && !state.getIsBrightInside() && !state.getIsOn());
    }

    private boolean bigIsTurnOff() {
        return (state.getIsPresence() && state.getIsBrightOutside() && state.getIsBrightInside() && state.getIsOn())
        || (!state.getIsPresence() && state.getIsBrightOutside() && state.getIsBrightInside() && state.getIsOn())
        || (!state.getIsPresence() && state.getIsBrightOutside() && !state.getIsBrightInside() && state.getIsOn())
        || (!state.getIsPresence() && !state.getIsBrightOutside() && state.getIsBrightInside() && state.getIsOn())
        || (!state.getIsPresence() && !state.getIsBrightOutside() && !state.getIsBrightInside() && state.getIsOn());
    }

    private void bigDecision(){
        if (bigIsTurnOn()){
            // critique, demander a l'interface d'allumer notre lumiere
        } else if (bigIsTurnOff()) {
            // critique, demander a l'interface d'eteindre notre lumiere
        }
    }

    //##################################################################################################################
    //table de karnaught pour chaque action, beaucoup plus simple et propre, plus dur a reverse engenieer

    private boolean isTurnOff(){
        // table de karnaugh pour les cas needTurnOff = 1 y = ((!A)D or BCD)
        return (!state.getIsPresence() && state.getIsBrightOutside())
                || (state.getIsBrightOutside() && state.getIsBrightInside() && state.getIsOn());
    }
    private boolean isTurnOn(){
        // table de karnaugh pour les cas needTurnOn = 1 y = A!C!D
        return (state.getIsPresence() && !state.getIsBrightInside() && !state.getIsOn());
    }

    private void decision(){
        if(isTurnOn()){
            // on part du principe qu'il n'y a pas besoin ici de tester si on boucle, car on veut dans le pire des cas
            // que les lumières soient allumées

            // critique, demander a l'interface d'allumer notre lampe
        } else if(isTurnOff()){
            StateLumiere nextState = new StateLumiere(state);
            nextState.toggleIsOn();
            if(!nextState.compareStates(this.lastState)) {
                // critique, demander a l'interface d'eteindre notre lampe
            } else {
                // on détecte un cas de bouclage : on rollback la valeur de lastState
                lastState = new StateLumiere(stateRollback);
            }
        }
    }

    //##################################################################################################################
}