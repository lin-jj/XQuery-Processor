import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XQueryExtendVisitor extends XQueryBaseVisitor<ArrayList<Node>> {
    private ArrayList<Node> context = new ArrayList();
    boolean isFilter = false;
    Document xmlDoc = null;
    
    public XQueryExtendVisitor(){
        try {
        	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = builderFactory.newDocumentBuilder();
        	xmlDoc = builder.newDocument();
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    //'doc' '(' '"' fileName '"' ')' '/' rp
    public ArrayList<Node> visitApSlash(XQueryParser.ApSlashContext ctx){
        visit(ctx.fileName());
        context = visit(ctx.rp());
        return context;
    }

    private static ArrayList<Node> getChildren(Node node){
        ArrayList<Node> children = new ArrayList<>();
        NodeList nl = node.getChildNodes();
        for(int i = 0; i < nl.getLength(); i++){
            children.add(nl.item(i));
        }
        return children;
    }

    @Override
    //ap -> 'doc' '(' '"' fileName '"' ')' '//' rp
    public ArrayList<Node> visitApDoubleSlash(XQueryParser.ApDoubleSlashContext ctx){
        visit(ctx.fileName());
        LinkedList<Node> list = new LinkedList<>(context);
        while(!list.isEmpty()){
            Node node = list.poll();
            ArrayList<Node> children = getChildren(node);
            list.addAll(children);
            context.addAll(children);
        }
        return visit(ctx.rp());
    }

    @Override
    // fileName -> NAME ('.' NAME)?
    public ArrayList<Node> visitFileName(XQueryParser.FileNameContext ctx) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch(ParserConfigurationException e){
            e.printStackTrace();
        }
        Document document = null;
        try {
            if(builder != null) {
                document = builder.parse(new File("test//j_caesar.xml"));
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        if(document != null){
            document.getDocumentElement().normalize();
        }

        context = new ArrayList<Node>();
        context.add(document);
        return context;
    }

    @Override
    //rp -> NAME
    public ArrayList<Node> visitRpName(XQueryParser.RpNameContext ctx) {
        ArrayList<Node> nodeArrayList = new ArrayList<>();
        String name = ctx.getText();
        for(Node node : context){
            ArrayList<Node> childrenArrayList = getChildren(node);
            for(Node child : childrenArrayList){
                if(child.getNodeName().equals(name)){
                    nodeArrayList.add(child);
                }
            }
        }
        context = nodeArrayList;
        return context;
    }

    @Override
    //rp -> '*'
    public ArrayList<Node> visitRpStar(XQueryParser.RpStarContext ctx) {
        ArrayList<Node> nodeArrayList = new ArrayList<>();
        for(Node node : context){
            nodeArrayList.addAll(getChildren(node));
        }
        context = nodeArrayList;
        return context;
    }

    @Override
    // rp -> '.'
    public ArrayList<Node> visitRpDot(XQueryParser.RpDotContext ctx) {
        return context;
    }

    @Override
    //rp -> '..'
    public ArrayList<Node> visitRpDoubleDot(XQueryParser.RpDoubleDotContext ctx) {
        HashSet<Node> nodeHashSet = new HashSet<>();
        for(Node node : context){
            if(node.getNodeType() == Node.DOCUMENT_NODE){
                continue;
            }
            nodeHashSet.add(node.getParentNode());
        }
        context = new ArrayList<>(nodeHashSet);
        return context;
    }

    @Override
    //rp -> 'text()'
    public ArrayList<Node> visitRpText(XQueryParser.RpTextContext ctx) {
        ArrayList<Node> rtn = new ArrayList<>();
        for(Node node : context){
            ArrayList<Node> nl = getChildren(node);
            for(Node child : nl){
                if(child.getNodeType() == Node.TEXT_NODE && !child.getNodeValue().trim().isEmpty() && !child.getNodeValue().equals("\n")){
                    rtn.add(child);
                }
            }
        }
        context = rtn;
        return rtn;
    }

    @Override
    //rp -> '@' NAME
    public ArrayList<Node> visitRpAttribute(XQueryParser.RpAttributeContext ctx) {
        String attriName = ctx.NAME().getText();
        ArrayList<Node> rtn = new ArrayList<>();
        for(Node node : context){
            if(node.getNodeType() == Node.ELEMENT_NODE ){
                Element e = (Element) node;
                if(!e.getAttribute(attriName).isEmpty()){
                    if(isFilter) {
                    	rtn.add(node);
                    }
                    else {
                    	rtn.add(node.getAttributes().getNamedItem(attriName));
                    	System.out.printf("attribute:: %s: %s\n", attriName, node.getAttributes().getNamedItem(attriName).getTextContent());
                    }
                }
            }
        }
        context = rtn;
        return rtn;
    }

    @Override
    //rp -> '(' rp ')'
    public ArrayList<Node> visitRpParentheses(XQueryParser.RpParenthesesContext ctx) {
        return visit(ctx.rp());
    }

    @Override
    //rp -> rp '/' rp
    public ArrayList<Node> visitRpSlash(XQueryParser.RpSlashContext ctx) {
        visit(ctx.rp(0));
        context = visit(ctx.rp(1));
        return context;
    }

    @Override
    // rp -> rp '//' rp
    public ArrayList<Node> visitRpDoubleSlash(XQueryParser.RpDoubleSlashContext ctx) {
        visit(ctx.rp(0));
        LinkedList<Node> list = new LinkedList<>(context);
        ArrayList<Node> rtn = new ArrayList<>();
        while(!list.isEmpty()){
            Node node = list.poll();
            ArrayList<Node> children = getChildren(node);
            list.addAll(children);
            context.addAll(children);
        }
        return visit(ctx.rp(1));
    }

    @Override
    //rp -> rp '[' f ']'
    public ArrayList<Node> visitRpFilter(XQueryParser.RpFilterContext ctx) {
        visit(ctx.rp());
        isFilter = true;
        ArrayList<Node> saveContext = new ArrayList<>(context);
        ArrayList<Node> newContext = new ArrayList<>();
        for(Node node : saveContext) {
        	context.clear();
        	context.add(node);
        	if(!visit(ctx.f()).isEmpty()) {
        		newContext.add(node);
        	}
        }
        context = newContext;
        isFilter = false;
        return context;
    }

    @Override
    //rp -> rp ',' rp
    public ArrayList<Node> visitRpComma(XQueryParser.RpCommaContext ctx) {
        ArrayList<Node> saveContext = new ArrayList<>(context);
        ArrayList<Node> res1 = visit(ctx.rp(0));
        context = saveContext;
        ArrayList<Node> res2 = visit(ctx.rp(1));
        ArrayList<Node> res = new ArrayList<>();
        res.addAll(res1);
        res.addAll(res2);
        context = res;
        return context;
    }

    @Override
    //f -> rp
    public ArrayList<Node> visitFilterRp(XQueryParser.FilterRpContext ctx) {
        ArrayList<Node> saveContext = new ArrayList<>(context);
        ArrayList<Node> res = visit(ctx.rp());
        context = saveContext;
        return res;
    }

    @Override
    // f -> rp eq|= rp
    public ArrayList<Node> visitFilterEqual(XQueryParser.FilterEqualContext ctx) {
        ArrayList<Node> saveContext = new ArrayList<>(context);
        ArrayList<Node> tempContext = new ArrayList<>();
        ArrayList<Node> rtn = new ArrayList<>();
        for(Node node : context){
            boolean flag = false;
            tempContext.clear();
            tempContext.add(node);
            context = tempContext;
            ArrayList<Node> res1 = visit(ctx.rp(0));
            context = tempContext;
            ArrayList<Node> res2 = visit(ctx.rp(1));
            for(Node node1 : res1){
                if(flag){
                    break;
                }
                for(Node node2 : res2){
                    if(flag){
                        break;
                    }
                    if(node1.isEqualNode(node2)){
                        rtn.add(node);
                        flag = true;
                    }
                }
            }
        }
        context = saveContext;
        return rtn;
    }

    @Override
    //f -> rp ==|is rp
    public ArrayList<Node> visitFilterIs(XQueryParser.FilterIsContext ctx) {
        ArrayList<Node> saveContext = new ArrayList<>(context);
        ArrayList<Node> tempContext = new ArrayList<>();
        ArrayList<Node> rtn = new ArrayList<>();
        for(Node node : context){
            boolean flag = false;
            tempContext.clear();
            tempContext.add(node);
            context = tempContext;
            ArrayList<Node> res1 = visit(ctx.rp(0));
            context = tempContext;
            ArrayList<Node> res2 = visit(ctx.rp(1));
            for(Node node1 : res1){
                if(flag){
                    break;
                }
                for(Node node2 : res2){
                    if(flag){
                        break;
                    }
                    if(node1.isSameNode(node2)){
                        rtn.add(node);
                        flag = true;
                    }
                }
            }
        }
        context = saveContext;
        return rtn;
    }

    @Override
    //f -> '(' f ')'
    public ArrayList<Node> visitFilterParentheses(XQueryParser.FilterParenthesesContext ctx) {
        return visit(ctx.f());
    }

    @Override
    //f -> f 'and' f
    public ArrayList<Node> visitFilterAnd(XQueryParser.FilterAndContext ctx) {
        ArrayList<Node> res1 = visit(ctx.f(0));
        ArrayList<Node> res2 = visit(ctx.f(1));
        HashSet<Node> hashSet = new HashSet<Node>(res1);
        ArrayList<Node> rtn = new ArrayList<>();
        for(Node node : res2){
            if(hashSet.contains(node)){
                rtn.add(node);
            }
        }
        return rtn;
    }

    @Override
    //f -> f 'or' f
    public ArrayList<Node> visitFilterOr(XQueryParser.FilterOrContext ctx) {
        ArrayList<Node> res1 = visit(ctx.f(0));
        ArrayList<Node> res2 = visit(ctx.f(1));
        HashSet<Node> hashSet = new HashSet<Node>(res1);
        hashSet.addAll(res2);
        return new ArrayList<>(hashSet);
    }

    @Override
    //f -> 'not' f
    public ArrayList<Node> visitFilterNot(XQueryParser.FilterNotContext ctx) {
        ArrayList<Node> res = visit(ctx.f());
        HashSet<Node> hashSet = new HashSet<>(res);
        ArrayList<Node> rtn = new ArrayList<>();
        for(Node node : context){
            if(!hashSet.contains(node)){
                rtn.add(node);
            }
        }
        return rtn;
    }
    
    private Node makeText(String s){
        return xmlDoc.createTextNode(s);
    }
    
    private Node makeElem(String t, ArrayList<Node> l){
        Node res = xmlDoc.createElement(t);
        for (Node n : l) {
            Node temp = xmlDoc.importNode(n, true);
            res.appendChild(temp);
        }
        return res;
    }
    
	@Override
	//XQ -> StringConstant
    public ArrayList<Node> visitXqStringConstant(XQueryParser.XqStringConstantContext ctx) { 
    	ArrayList<Node> res = new ArrayList<>();
    	res.add(makeText(ctx.StringConstant().getText()));
    	return res; 
    }
    
	@Override
	//XQ -> ap
	public ArrayList<Node> visitXqAp(XQueryParser.XqApContext ctx) { 
		return visit(ctx.ap());
	}
    
	@Override
	//XQ -> '(' XQ ')'
	public ArrayList<Node> visitXqParentheses(XQueryParser.XqParenthesesContext ctx) { 
		return visit(ctx.xq());
	}
    
	@Override
	//XQ -> XQ ',' XQ
	public ArrayList<Node> visitXqComma(XQueryParser.XqCommaContext ctx) { 
        ArrayList<Node> saveContext = new ArrayList<>(context);
        ArrayList<Node> res1 = visit(ctx.xq(0));
        context = saveContext;
        ArrayList<Node> res2 = visit(ctx.xq(1));
        ArrayList<Node> res = new ArrayList<>();
        res.addAll(res1);
        res.addAll(res2);
        context = res;
        return context;
	}

	@Override
	//XQ -> '<' NAME '>' '{' xq '}' '<' '/' NAME '>'
	public ArrayList<Node> visitXqName(XQueryParser.XqNameContext ctx) {
		ArrayList<Node> res = new ArrayList<>();
        res.add(makeElem(ctx.NAME(0).getText(), visit(ctx.xq())));
        return res;
	}
}
