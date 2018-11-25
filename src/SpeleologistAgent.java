import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Random;

public class SpeleologistAgent extends Agent {

    private AID navigatorId; // NavigatorAgent
    private AID enviromentId; // EnvironmentAgent


    @Override
    protected void setup() {
        System.out.println("Speleologist agent " + getAID().getLocalName() + " is ready!");
        addBehaviour(FindAndSetAgents());
    }

    @Override
    protected void takeDown() {
        System.out.println("Speleologist agent terminating...");
    }

    private WakerBehaviour FindAndSetAgents() {
        return new WakerBehaviour(this, 5000) {
            @Override
            protected void onWake() {
                DFAgentDescription navigatorDescription = new DFAgentDescription();
                DFAgentDescription enviromentDescription = new DFAgentDescription();
                ServiceDescription navigatorSevice = new ServiceDescription();
                ServiceDescription enviromentService = new ServiceDescription();
                navigatorSevice.setType("Wumpus-World-Navigator");
                enviromentService.setType("Wumpus-World-Environment");
                navigatorDescription.addServices(navigatorSevice);
                enviromentDescription.addServices(enviromentService);
                try {
                    navigatorId = DFService.search(myAgent, navigatorDescription)[0].getName();
                    enviromentId = DFService.search(myAgent, enviromentDescription)[0].getName();
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                myAgent.addBehaviour(new EviromentWanderingBehaviour());
            }
        };
    }

    private class EviromentWanderingBehaviour extends Behaviour {

        private int step = 0;
        private MessageTemplate mt;
        private String message;

        private String[] dict = {"There is a %s here. ",
                "I feel %s here. ",
                "It's a %s here. "};

        @Override
        public void action() {
            switch (step) {
                case 0:
                    CreateRequestToEviroment();
                    break;
                case 1:
                    SaveEnviromentState();
                    break;
                case 2:
                    CreateRequestToNavigator();
                    break;
                case 3:
                    GetNavigatorResponce();
                    break;
                case 4:
                    CreateActionAndSendToEnviroment();
                    break;
                case 5:
                    IsAlreadyClimbed();
                    break;
            }
        }

        private void CreateRequestToEviroment() {
            ACLMessage requestPercept = new ACLMessage(ACLMessage.REQUEST);
            requestPercept.addReceiver(enviromentId);
            requestPercept.setConversationId("percept");
            myAgent.send(requestPercept);
            System.out.println(getAID().getLocalName() + ": Gathering information about Environment.");
            mt = MessageTemplate.MatchConversationId("percept");

            step++;
        }

        private void SaveEnviromentState() {
            ACLMessage reply = myAgent.receive(mt);
            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.INFORM) {
                    message = reply.getContent().concat("What do I need to do?");
                    step++;
                }
            } else
                block();
        }

        private void CreateRequestToNavigator() {
            ACLMessage askForAction = new ACLMessage(ACLMessage.REQUEST);
            askForAction.addReceiver(navigatorId);
            askForAction.setContent(message);
            askForAction.setConversationId("Ask-for-action");
            myAgent.send(askForAction);
            System.out.println(getAID().getLocalName() + ": " + message);
            mt = MessageTemplate.MatchConversationId("Ask-for-action");
            step++;
        }

        private void GetNavigatorResponce() {
            ACLMessage reply2 = myAgent.receive(mt);
            if (reply2 != null) {
                if (reply2.getPerformative() == ACLMessage.PROPOSE) {
                    message = reply2.getContent();
                    step++;
                }
            } else {
                block();
            }
        }

        private void CreateActionAndSendToEnviroment() {
            ACLMessage action = new ACLMessage(ACLMessage.CFP);
            action.addReceiver(enviromentId);
            action.setContent(message);
            action.setConversationId("action");
            myAgent.send(action);
            System.out.println(getAID().getLocalName() + ": " + message);
            mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId("action"),
                    MessageTemplate.MatchInReplyTo(action.getReplyWith()));
            step++;
        }

        private void IsAlreadyClimbed() {
            if (message == "Climb") {
                step++;
                doDelete();
                return;
            } else
                step = 0;
        }

        @Override
        public boolean done() {
            return step == 6;
        }
    }
}

