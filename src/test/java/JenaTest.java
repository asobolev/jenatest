import java.util.*;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.*;
import com.hp.hpl.jena.vocabulary.*;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.*;

/**
 * Created by andrey on 22.04.15.
 */
public class JenaTest {

    private static final String schemaPath = "testfiles/foaf_schema.rdf";
    private static final String dataPath = "testfiles/foaf_example.rdf";

    public static String tbl = "http://www.w3.org/People/Berners-Lee/card#i";
    public static String rhm = "http://dig.csail.mit.edu/2007/wiki/people/RobertHoffmann#RMH";
    private String foaf = "http://xmlns.com/foaf/0.1/";

    private Model model;
    private OntModel ontology;
    private InfModel infm;

    private Model loadModelFromResources(String path) {
        ClassLoader cl = JenaTest.class.getClassLoader();
        return RDFDataMgr.loadModel(cl.getResource(path).toString());
    }

    @Before
    public void setUp() throws Exception {

        // load ontology
        ontology = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
        ontology.addSubModel(loadModelFromResources(schemaPath));

        // load instances
        model = loadModelFromResources(dataPath);

        // build inferred model
        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        reasoner = reasoner.bindSchema(ontology);

        infm = ModelFactory.createInfModel(reasoner, model);
    }

    /**
     *  Test what happens with Model / Ontology if InfModel changes
     *
     *  ...Having bound a Model into an InfModel by using a Reasoner its content
     *  can still be changed by the normal add and remove calls to the InfModel.
     *  Any such change the model will usually cause all current deductions and
     *  temporary rules to be discarded and inference will start again from
     *  scratch at the next query..
     */
    @Test
    public void testChangeInfModel() throws Exception {
        UpdateVarListener lsn = new UpdateVarListener();
        model.register(lsn);

        Resource res = ResourceFactory.createResource(UUID.randomUUID().toString());
        String foaf = ontology.getNsPrefixMap().get("foaf");

        Literal foo = ResourceFactory.createPlainLiteral("foo");
        Statement st = ResourceFactory.createStatement(res, ontology.getOntProperty(foaf + "name"), foo);

        // add triple
        infm.add(st);

        assert infm.contains(st);
        assert model.contains(st);
        assert !ontology.contains(st);
        assert lsn.hasChanged();

        // remove triple
        lsn.reset();
        infm.remove(st);

        assert !infm.contains(st);
        assert !model.contains(st);
        assert !ontology.contains(st);
        assert lsn.hasChanged();
    }


    /**
     *  Test what happens with InfModel if Model changes
     *
     *  from https://jena.apache.org/documentation/inference/
     *
     *  ...There are times when the data in a model bound into an InfModel can is changed
     *  "behind the scenes" instead of through calls to the InfModel. If this occurs
     *  the result of future queries to the InfModel are unpredictable. To overcome
     *  this and force the InfModel to reconsult the raw data use the InfModel.rebind()
     *  call..
     */
    @Test
    public void testChangeModel() throws Exception {
        UpdateVarListener lsn = new UpdateVarListener();
        infm.register(lsn);

        Resource res = ResourceFactory.createResource(UUID.randomUUID().toString());
        String foaf = ontology.getNsPrefixMap().get("foaf");

        Literal foo = ResourceFactory.createPlainLiteral("foo");
        Statement st = ResourceFactory.createStatement(res, ontology.getOntProperty(foaf + "name"), foo);

        // add triple
        model.add(st);

        assert model.contains(st);
        assert !ontology.contains(st);
        assert infm.contains(st); // good news
        assert !lsn.hasChanged(); // WTF?

        infm.rebind();

        assert infm.contains(st);
        assert !lsn.hasChanged(); // WTF?

    }

    /**
     *  Test what happens with InfModel if Ontology changes
     */
    @Test
    public void testChangeOntology() throws Exception {
        UpdateVarListener lsn = new UpdateVarListener();
        infm.register(lsn);

        String foaf = ontology.getNsPrefixMap().get("foaf");

        // create a super-Property to query for it later
        Property sp = ontology.createProperty(foaf, "attribute");
        Property kp = ontology.getProperty(foaf + "name");
        ontology.add(kp, RDFS.subPropertyOf, sp);

        Resource tblRes = infm.getResource(tbl);

        // new inferences are not in the graph
        assert tblRes.getProperty(sp) == null;
        assert !lsn.hasChanged();

        infm.rebind();

        // even after rebind
        assert tblRes.getProperty(sp) == null;
        assert !lsn.hasChanged();

        // only full replace of InfModel can work
        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        reasoner = reasoner.bindSchema(ontology);

        infm = ModelFactory.createInfModel(reasoner, model);
        tblRes = infm.getResource(tbl);

        assert tblRes.getProperty(sp) != null;
    }
}
