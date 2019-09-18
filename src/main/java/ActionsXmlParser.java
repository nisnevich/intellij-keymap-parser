import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import entities.Action;
import entities.Keymap;
import entities.Shortcut;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ActionsXmlParser {

    final static String COMMUNITY_PROJECT_ROOT = "/Users/arseniy.nisnevich/IdeaProjects/intellij-community";
    final static boolean CLEANUP = true;

    final static Logger logger = Logger.getLogger(ActionsXmlParser.class);
//    private Gson gson = new Gson();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static final String TEXT = "text";
    public static final String ACTION = "action";
    public static final String ID = "id";
    public static final String GROUP = "group";
    public static final String DESCRIPTION = "description";
    public static final String USE_SHORTCUT_OF = "use-shortcut-of";
    String keymapsDir = COMMUNITY_PROJECT_ROOT + "/platform/platform-resources/src/keymaps";
    String actionsPath = COMMUNITY_PROJECT_ROOT + "/platform/platform-resources/src/keymaps/Mac OS X 10.5+.xml";
    String[] descriptionPaths = new String[] {
            COMMUNITY_PROJECT_ROOT + "/platform/platform-resources/src/idea/VcsActions.xml",
            COMMUNITY_PROJECT_ROOT + "/platform/platform-resources/src/idea/PlatformActions.xml",
            COMMUNITY_PROJECT_ROOT + "/platform/platform-resources/src/idea/LangActions.xml",
            COMMUNITY_PROJECT_ROOT + "/platform/platform-resources/src/idea/ExternalSystemActions.xml",
            COMMUNITY_PROJECT_ROOT + "/platform/platform-resources/src/idea/PlatformLangActionManager.xml",
    };
    String propertiesDescriptionPath = COMMUNITY_PROJECT_ROOT + "/platform/platform-resources-en/src/messages/ActionsBundle.properties";

    public void parseActions() throws HackathonException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        List<Document> descrDocs = new ArrayList<>();
        for (String path : descriptionPaths) {
            descrDocs.add(getDoc(docFactory, new File(path)));
        }

        File keymapsFolder = new File(keymapsDir);
        for (final File keymapFile : keymapsFolder.listFiles(file -> !file.isHidden())) {
            if (keymapFile.isDirectory()) {
                throw new HackathonException("Make sure keymaps dir points the right path.");
            } else {
                Document actionsDoc = getDoc(docFactory, keymapFile);
                parseFile(actionsDoc, descrDocs);
            }
        }
    }

    private void parseFile(Document actionsDoc, List<Document> descrDocs) throws HackathonException {
        Keymap keymap = new Keymap();
        keymap.setName(actionsDoc.getDocumentElement().getAttributes().getNamedItem("name").getNodeValue());
        List<Node> actions = XmlUtil.asList(actionsDoc.getDocumentElement().getChildNodes());
        List<Action> actionsList = new ArrayList<>();
        int actionsCount = 0;
        try (InputStream input = new FileInputStream(propertiesDescriptionPath)) {
            Properties properties = new Properties();
            properties.load(input);
            for (Node actionNode : actions) {
                if (isNodeTextOrComment(actionNode)) continue;
                Action action = new Action();
                actionsCount++;
                action.setId(actionNode.getAttributes().getNamedItem("id").getNodeValue());
                List<Shortcut> shortcuts = new ArrayList<>();
                for (Node keyboardShortcut : XmlUtil.asList(actionNode.getChildNodes())) {
                    if (isNodeTextOrComment(keyboardShortcut)) continue;
                    if (isWeirdShortcutType(keyboardShortcut)) continue;
                    Shortcut shortcut = new Shortcut(keyboardShortcut.getAttributes().getNamedItem("first-keystroke").getNodeValue());
                    if (keyboardShortcut.getAttributes().getNamedItem("second-keystroke") != null) {
                        shortcut.setSecondKeystroke(keyboardShortcut.getAttributes().getNamedItem("second-keystroke").getNodeValue());
                    }
                    shortcuts.add(shortcut);
                }
                action.setShortcuts(shortcuts);

                boolean descriptionFoundInXml = setActionDescription(action, actionsDoc, descrDocs);
                boolean descriptionFoundInProperties = false;

                String propertiesKey = "action." + action.getId() + ".text";
                if (properties.containsKey(propertiesKey)) {
                    if (descriptionFoundInXml) {
                        logger.error("ID presented in both XML and property files: " + action.getId());
                    }
                    descriptionFoundInProperties = true;
                    action.setDescription(properties.getProperty(propertiesKey));
                }

                if (!descriptionFoundInXml && !descriptionFoundInProperties) continue;
                actionsList.add(action);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (CLEANUP) {
            for (Action action : actionsList) {
                action.setDescription(action.getDescription().replaceAll("_", ""));
                action.setId(action.getId().replaceAll("\\$", ""));
            }
        }

        keymap.setActions(actionsList);
        try {
            logger.info(String.format("Total matches for %s: %d of %d", keymap.getName(), actionsList.size(), actionsCount));
            FileWriter fileWriter = new FileWriter("keymaps/json-keymaps/" + keymap.getName() + ".json");
            gson.toJson(keymap, fileWriter);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    interface ITextSetter {
        void set(Action action, String text);
    }

    private boolean isWeirdShortcutType(Node keyboardShortcut) {
        return keyboardShortcut.getNodeName().equals("mouse-shortcut") || keyboardShortcut.getNodeName().equals("keyboard-gesture-shortcut");
    }

    private boolean setActionDescription(Action action, Document actionsDoc, List<Document> descrDocs) throws HackathonException {
        for (Document descrDoc : descrDocs) {
            Node actionsNode = descrDoc.getDocumentElement().getChildNodes().item(1);
            for (Node node : XmlUtil.asList(actionsNode.getChildNodes())) {
                if (isNodeTextOrComment(node)) continue;
                if (node.getNodeName().equals(ACTION)) {
                    if (hasAttr(node, ID) && getAttr(node, ID).equals(action.getId())) {
                        setHumanReadableName(action, node, "Description", Action::setDescription);
                    } else if (hasAttr(node, USE_SHORTCUT_OF) && getAttr(node, USE_SHORTCUT_OF).equals(action.getId())) {
                        setHumanReadableName(action, node, "Description", Action::setDescription);
                    }
                    logger.trace("Group name is missing for action " + action.getId());
                } else if (node.getNodeName().equals(GROUP)) {
                    for (Node actionNode : XmlUtil.asList(actionsNode.getChildNodes())) {
                        if (isNodeTextOrComment(actionNode)) continue;
                        if (hasAttr(actionNode, ID) && getAttr(actionNode, ID).equals(action.getId())) {
                            setHumanReadableName(action, actionNode, "Description", Action::setDescription);
                            setHumanReadableName(action, actionNode, "Group name", Action::setActionGroup);
                        } else if (hasAttr(actionNode, USE_SHORTCUT_OF) && getAttr(actionNode, USE_SHORTCUT_OF).equals(action.getId())) {
                            setHumanReadableName(action, actionNode, "Description", Action::setDescription);
                            setHumanReadableName(action, actionNode, "Group name", Action::setActionGroup);
                        }
                    }
                } else if (node.getNodeName().equals("reference")) {
                    // ignore
                } else {
                    throw new HackathonException("Unknown action type: " + node.getNodeName());
                }
            }
        }
        return action.getDescription() != null;
    }

    private boolean isNodeTextOrComment(Node actionNode) {
        return actionNode.getNodeType() == 3 || actionNode.getNodeType() == 8;
    }

    private void setHumanReadableName(Action action, Node keyboardShortcut, String what, ITextSetter textSetter) {
        if (hasAttr(keyboardShortcut, TEXT)) {
            textSetter.set(action, getAttr(keyboardShortcut, TEXT));
        } else if (hasAttr(keyboardShortcut, DESCRIPTION)) {
            textSetter.set(action, getAttr(keyboardShortcut, DESCRIPTION));
        } else {
            logger.log(Level.TRACE, what + " is missing for action " + action.getId());
        }
    }

    private String getAttr(Node node, String attr) {
        return node.getAttributes().getNamedItem(attr).getNodeValue();
    }

    private boolean hasAttr(Node node, String attr) {
        return node.getAttributes().getNamedItem(attr) != null;
    }

    private Document getDoc(DocumentBuilderFactory factory, File actionsXml) {
        Document actionsDoc = null;
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            actionsDoc = docBuilder.parse(actionsXml);
            actionsDoc.getDocumentElement().normalize();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return actionsDoc;
    }

    public static void main(String[] args) {
        try {
            new ActionsXmlParser().parseActions();
        } catch (HackathonException e) {
            e.printStackTrace();
        }
    }
}
