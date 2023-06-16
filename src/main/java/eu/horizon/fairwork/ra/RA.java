package eu.horizon.fairwork.ra;

import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class RA extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static JSONParser parser = new JSONParser(); // declares a static instance of JSONParser

	DFAgentDescription dfd = new DFAgentDescription(); // declares an instance of DFAgentDescription

	ACLMessage message; // declares an instance of ACLMessage
	Object[] order = null; // declares an array of objects named 'order' and initialize it as null
	AID myLastClient; // declares an instance of AID (Agent IDentifier), unique ID for an agent in the
						// Multi-Agent System
						// to store the AID of the last client this agent interacted with
	JSONArray restCall;
	Client client = Client.create();
	WebResource webResource;
	ClientResponse response;
	JSONArray associatedSkills;
	String Endpoint, Token, HomePosition;

	protected void setup() {
		System.out.println("I'm (RA) alive!!!");
		Object obj; // declares an object for holding the parsed JSON file
		try {
			obj = parser.parse(new FileReader(".\\src\\main\\resources\\coffeeMachine.json")); // parses JSON file named
																								// 'coffeeMachine.json'
																								// and assign it to the
																								// object

			JSONObject jsonObject = (JSONObject) obj; // casts the object to JSONObject
			Endpoint = (String) jsonObject.get("Endpoint");
			Token = (String) jsonObject.get("Token");
			HomePosition = (String) jsonObject.get("HomePosition");

			associatedSkills = (JSONArray) jsonObject.get("AssociatedSkills"); // gets the value associated with the key
																				// 'AssociatedSkills' from the
																				// JSONObject and cast it to JSONArray
			Iterator<JSONObject> iterator = associatedSkills.iterator(); // declares an iterator to traverse through the
																			// JSONArray
			JSONObject skill; // declares a JSONObject to store each skill
			String id, action; // declares strings to store the id, action, and REST Call of each skill
			while (iterator.hasNext()) { // while there is a next element in the JSONArray, it goes
				try {
					skill = iterator.next(); // gets the next skill
					id = (String) skill.get("ID"); // gets the value associated with the key 'ID' from the skill and
													// cast it to string
					action = (String) skill.get("Action"); // get the value associated with the key 'Action' from the
															// skill and cast it to string
					ServiceDescription service = new ServiceDescription(); // declares a ServiceDescription to describe
																			// a service
					service.setType(id); // sets the type of the service to be the id
					service.setName(action); // sets the name of the service to be the action
					dfd.addServices(service); // adds the service to the DFAgentDescription object
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			registerService();
		} catch (IOException | ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// process incoming REQUEST messages by retrieving an order, finding an
		// associated agent, and requesting a process list
		// process incoming INFORM messages by executing operations from the received
		// list and informing the original requester that the job is done
		addBehaviour(new Behaviour() {

			@Override
			public boolean done() {
				// TODO Auto-generated method stub
				return false; // behaviour never finishes
			}

			@Override
			public void action() { // main method to perform the behaviour
				message = receive(); // receives message
				if (message != null) {
					switch (message.getPerformative()) {
					case ACLMessage.REQUEST: // if the message is of the performative type REQUEST
						myLastClient = message.getSender(); // stores the sender of the message as last client
						try { // try to ...
							order = (Object[]) message.getContentObject(); // gets the content of the message and cast
																			// it to an object array
						} catch (UnreadableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println(order[0]);
						AID resourceAgent = searchForAgent(myAgent, (String) order[0]); // searches for an agent that
																						// can fulfill the order

						ACLMessage messageToSend = new ACLMessage(ACLMessage.REQUEST); // creates new REQUEST message
						messageToSend.addReceiver(resourceAgent); // adds the found agent as the receiver of the message
						messageToSend.setContent("PROCESS_LIST"); // sets the content of the message to "PROCESS_LIST"
						send(messageToSend); // sends message
						break;
					case ACLMessage.INFORM: // if the message is of the performative type INFORM
						System.out.println("Recebi do PTA o inform list.");
						JSONArray fwOperationList = null; // initializes a null JSONArray
						try {
							fwOperationList = (JSONArray) message.getContentObject(); // gets the content of the message
																						// and cast it to an object
																						// array
						} catch (UnreadableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Iterator<JSONObject> iterator = fwOperationList.iterator(); // creates an iterator for the
																					// JSONArray
						Iterator<JSONObject> iteratorRestCall;
						JSONObject operation; // declares a JSONObject
						String id, associatedSkill; // declares 'id' and 'AssociatedSkill' strings
						int i = 0;
						JSONObject skill;
						while (iterator.hasNext()) { // while there is a next item in the JSONArray, it keeps on
							try {
								operation = iterator.next(); // gets the next item from the JSONArray
								id = (String) operation.get("ID"); // gets the value associated with the key 'ID' and
																	// casts it to a string
								if (operation.get("Optional").equals("No")) { // if the operation isn't optional, it
																				// goes here (coffee selection)
									System.out.println("Executing: " + id);
									Iterator<JSONObject> iterator_skills = associatedSkills.iterator();
									while (iterator_skills.hasNext()) {
										skill = iterator_skills.next();
										if (id.equals(skill.get("Action"))) {
											JSONArray restOperationList = (JSONArray) skill.get("REST_CALL");
											System.out.println(restOperationList);
											iteratorRestCall = restOperationList.iterator();
											String rest_call;
											List<String> calls = restOperationList;
											for (int j = 0; j < calls.size(); j++) {
												System.out.println(calls.get(j));
												webResource = client.resource(Endpoint + Token + calls.get(j));
												response = webResource.type("application/json")
														.post(ClientResponse.class, "");
											}
										}
									}
								} else { // if the operation is optional, it goes here (season selection)
									if (order[i].toString().equals("no")) { // when "no" is chosen, it goes here
										System.out.println("Skipping operation: " + id);
									} else { // when a season is chosen, it goes here
										System.out.println("Executing: " + order[i]);
										Iterator<JSONObject> iterator_skills = associatedSkills.iterator();
										while (iterator_skills.hasNext()) {
											skill = iterator_skills.next();
											if (id.equals(skill.get("Action"))) {
												JSONArray restOperationList = (JSONArray) skill.get("REST_CALL");
												System.out.println(restOperationList);
												iteratorRestCall = restOperationList.iterator();
												String rest_call;
												List<String> calls = restOperationList;
												for (int j = 0; j < calls.size(); j++) {
													System.out.println(calls.get(j));
													webResource = client.resource(Endpoint + Token + calls.get(j));
													response = webResource.type("application/json")
															.post(ClientResponse.class, "");
												}
											}
										}
									}
								}
								i++;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						webResource = client.resource(Endpoint + Token + HomePosition);
						response = webResource.type("application/json")
								.post(ClientResponse.class, "");
						
						System.out.println("hello");
						ACLMessage replyMessage = new ACLMessage(ACLMessage.INFORM);
						replyMessage.addReceiver(myLastClient);
						replyMessage.setContent("JOB_DONE");
						myAgent.send(replyMessage);

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

	// method for registering the services
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

	public static AID searchForAgent(Agent agent, String agentType) {
		DFAgentDescription[] agentName = null;
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();

		// search in DF for an agent type

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