import aima.core.environment.wumpusworld.AgentPercept;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class EnvironmentAgent extends Agent {

    private AID spelAgent;
    private WumpusWorldEnv wwe;


    @Override
    protected void setup() {
        System.out.println("Environment agent " + getAID().getName() + " is ready!");

        wwe = new WumpusWorldEnv();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Wumpus-World-Cave");
        sd.setName("Cave-wandering");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new RequestBehavior());
    }
    
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment agent terminating...");
    }
    private class RequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (spelAgent == null)
                    spelAgent = msg.getSender();
                if (spelAgent.equals(msg.getSender())) {
                    if (msg.getPerformative() == ACLMessage.REQUEST)
                        myAgent.addBehaviour(new PerceptReplyBehaviour(msg));
                    if (msg.getPerformative() == ACLMessage.CFP)
                        myAgent.addBehaviour(new WorldChangingBehaviour(msg));
                }
                else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                    myAgent.send(reply);
                }
            }
            else
            {
                block();
            }
        }
    }

    private class PerceptReplyBehaviour extends OneShotBehaviour {

        ACLMessage msg;

        public PerceptReplyBehaviour(ACLMessage m)
        {
            super();
            msg = m;
        }

        public void action() {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(GeneratePerceptSequence());
            myAgent.send(reply);
            System.out.println(getAID().getLocalName() + ": " + reply.getContent());
        }

        private String GeneratePerceptSequence() {
            StringBuilder reply = new StringBuilder();
            reply.append("[");
            AgentPercept ap = wwe.getPercept();
            if (ap.isStench())
                reply.append("Stench, ");
            else
                reply.append("None, ");
            if (ap.isBreeze())
                reply.append("Breeze, ");
            else
                reply.append("None, ");
            if (ap.isGlitter())
                reply.append("Glitter, ");
            else
                reply.append("None, ");
            if (ap.isBump())
                reply.append("Bump, ");
            else
                reply.append("None, ");
            if (ap.isScream())
                reply.append("Scream]");
            else
                reply.append("None]");
            return reply.toString();
        }
    }

    private class WorldChangingBehaviour extends OneShotBehaviour {

        ACLMessage msg;

        public WorldChangingBehaviour(ACLMessage m)
        {
            super();
            msg = m;
        }

        public void action() {
            String content = msg.getContent().toLowerCase();
            if (content.contains("forward"))
                wwe.ChangeWorld("Forward");
            else if (content.contains("shoot"))
                wwe.ChangeWorld("Shoot");
            else if (content.contains("climb"))
                wwe.ChangeWorld("Climb");
            else if (content.contains("grab"))
                wwe.ChangeWorld("Grab");
            else if (content.contains("right"))
                wwe.ChangeWorld("TurnRight");
            else if (content.contains("left"))
                wwe.ChangeWorld("TurnLeft");
        }
    }
}
