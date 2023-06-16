package eu.horizon.fairwork.pta;

import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
//import org.json.simple.JSONObject;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class PTA extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static JSONParser parser = new JSONParser();	//initialize a new JSONParser object, which is used to parse JSON strings into Java objects
	static JSONArray fwOperationList;				//declare a JSONArray object

	DFAgentDescription dfd = new DFAgentDescription();	//describe agents in the Directory Facilitator (DF)

	ACLMessage message;		//messages that agents send to each other (requests, responses, confirmations ...)

	protected void setup() {	//invoked when the agent starts
		System.out.println("I'm alive!!!");
		Object obj;				//initialize an object for holding the parsed JSON file
		try {
			obj = parser.parse(new FileReader(".\\src\\main\\resources\\" + getLocalName() + ".json"));	 //parse the JSON file corresponding to the PTA agent's local name and assign it to obj
			System.out.println(obj);	//print parsed JSON file
			JSONObject jsonObject = (JSONObject) obj;	//cast obj into a JSONObject

			JSONObject fwProcess = (JSONObject) jsonObject.get("FWProcess");  //gets the 'FWProcess' object from the parsed JSON

			fwOperationList = (JSONArray) fwProcess.get("FWOperationList");  //gets the 'FWOperationList' array from the 'FWProcess' object
			Iterator<JSONObject> iterator = fwOperationList.iterator();		//initializes an iterator to iterate over the 'FWOperationList' array
			
			JSONObject skill;				
			String id, associatedSkill;
			
			//loop over the FWOperationList array
			while (iterator.hasNext()) {
				try {
					skill = iterator.next();		//gets the next object in the 'FWOperationList' array
					id = (String) skill.get("ID");	//gets the 'ID' from the 'skill' object
					associatedSkill = (String) skill.get("AssociatedSkill");	//gets the 'AssociatedSkill' from the 'skill' object
					
					//creates a new service description object setting its type and name
					ServiceDescription service = new ServiceDescription();	//
					service.setType(id);
					service.setName(associatedSkill);
					
					dfd.addServices(service);	//add the service to the DFAgentDescription object
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			registerService();	//calls function to register the service
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//listen for incoming messages answering with an INFROM when it receives a message with a REQUEST performative and content "PROCESS_LIST"
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
					case ACLMessage.REQUEST:
						if (message.getContent().equals("PROCESS_LIST")) {
							ACLMessage replyMessage = new ACLMessage(ACLMessage.INFORM);
							replyMessage.addReceiver(message.getSender());
							try {
								replyMessage.setContentObject(fwOperationList);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							myAgent.send(replyMessage);
							System.out.println("Informei do process list.");
						}
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

	protected void registerService() {

		boolean executed_registration = false;
		try {
			DFService.register(this, dfd);
			executed_registration = true;
			System.out.println("Service successfully registered!");
		} catch (FIPAException e) {
			System.err.println(getLocalName() + " registration with DF unsucceeded (1st time).");
		}

		if (!executed_registration) {
			try {
				DFService.deregister(this, dfd);
				DFService.register(this, dfd);
				System.out.println("Service successfully registered!");
			} catch (Exception e) {
				System.err.println(getLocalName() + " registration with DF unsucceeded (2nd time).");
			}
		}
	}
}