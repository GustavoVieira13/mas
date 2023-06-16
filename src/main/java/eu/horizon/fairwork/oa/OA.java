package eu.horizon.fairwork.oa;

import java.io.IOException;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class OA extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ACLMessage message;

	protected void setup() {
//		
		Object[] args = getArguments(); //receives the arguments passed to the agent

		if (args != null && args.length > 0) {
			System.out.println(args[0] + ";" + args[1] + ";" + args[2] + ";");
			AID resourceAgent = searchForAgent(this, "robot");		//search for the agent that can make its coffee
			ACLMessage messageToSend = new ACLMessage(ACLMessage.REQUEST);
			messageToSend.addReceiver(resourceAgent);
			try {
				messageToSend.setContentObject(args);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			send(messageToSend);
			System.out.println("I've sent a message to: " + resourceAgent.getLocalName());
		}
		
		//listen for incoming messages, and when it receives an INFORM message, it prints "I'm gonna enjoy my delicious drink!"
		addBehaviour(new Behaviour() {

			@Override
			public boolean done() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void action() {
				message = receive();
				if (message != null) {
					switch (message.getPerformative()) {
					case ACLMessage.INFORM:
						System.out.println("I'm gonna enjoy my delicious drink!");
						break;
					default:
						break;
					}
				} else {
					block();
				}
			}
		});
	}

	public static AID searchForAgent(Agent agent, String agentType) {
		DFAgentDescription[] agentName = null;
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();

		// search in the DF for an agent type
		sd.setType(agentType);
		dfd.addServices(sd);
		try {
			agentName = DFService.search(agent, agent.getDefaultDF(), dfd);
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return agentName[0].getName();
	}
}
