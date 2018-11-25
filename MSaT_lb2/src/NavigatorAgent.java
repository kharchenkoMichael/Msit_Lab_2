import aima.core.agent.Action;
import aima.core.agent.impl.DynamicAction;
import aima.core.environment.wumpusworld.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jdk.jshell.spi.ExecutionControl;

import java.util.Random;

public class NavigatorAgent extends Agent {

    private AID spelAgent;

    private HybridWumpusAgent agentLogic;

    private String[] dict = {"You need to %s.",
        "You should %s.",
        "What you need to do is to %s."};

    @Override
    protected void setup() {

        agentLogic = new HybridWumpusAgent();

        System.out.println("Navigator agent " + getAID().getLocalName() + " is ready!");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Wumpus-World-Navigator");
        sd.setName("Wumpus-Gold-finder");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new PerceptRequestBehavior());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator agent terminating...");
    }

    private class PerceptRequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Ask-for-action")));
            if (msg != null) {
                if (spelAgent == null)
                    spelAgent = msg.getSender();
                if (spelAgent.equals(msg.getSender()))
                    myAgent.addBehaviour(new CreateSendPropose(msg));
                else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
            }
            else
            {
                block();
            }
        }
    }

    private class CreateSendPropose extends OneShotBehaviour {

        ACLMessage msg;

        public  CreateSendPropose(ACLMessage m)
        {
            super();
            msg = m;
        }

        public void action() {
            String content = msg.getContent();
            ACLMessage reply = msg.createReply();
            if (content != null) {
                reply.setPerformative(ACLMessage.PROPOSE);
                Action act = agentLogic.execute(ExtractPercept(content));
                reply.setContent(GenerateActionMessage(act));
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("not-available");
            }
            myAgent.send(reply);
            System.out.println(getAID().getLocalName() + ": " + reply.getContent());
        }

        private String GenerateActionMessage(Action act) {
            String a = ((DynamicAction)act).getName();
            if (a.contains("Turn"))
                a = "turn " + a.toLowerCase().substring(4);
            else if (a.equals("Forward"))
                    a = "go forward";
            else
                a = a.toLowerCase();
            return String.format(dict[new Random().nextInt(3)], a);
        }

        private AgentPercept ExtractPercept(String content) {
            content = content.toLowerCase();
            AgentPercept res = new AgentPercept(content.contains("stench"),
                    content.contains("breeze"),
                    content.contains("glitter"),
                    content.contains("bump"),
                    content.contains("scream"));
            return res;
        }
    }
}
