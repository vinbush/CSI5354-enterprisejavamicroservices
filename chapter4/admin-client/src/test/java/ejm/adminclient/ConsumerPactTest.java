package ejm.adminclient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.dius.pact.consumer.ConsumerPactTestMk2;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.Request;
import au.com.dius.pact.model.RequestResponseInteraction;
import au.com.dius.pact.model.RequestResponsePact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.fest.assertions.Assertions;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Ken Finnigan
 */
public class ConsumerPactTest extends ConsumerPactTestMk2 {
    private Category createCategory(Integer id, String name) {
        Category cat = new TestCategoryObject(id, LocalDateTime.parse("2002-01-01T00:00:00"), 1);
        cat.setName(name);
        cat.setVisible(Boolean.TRUE);
        cat.setHeader("header");
        cat.setImagePath("n/a");

        return cat;
    }

    private Category createDummyCategory(Integer id) {
        Category cat = new TestCategoryObject(id, null, null);
        return cat;
    }

    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        Category top = createCategory(0, "Top");

        Category transport = createCategory(1000, "Transportation");
        transport.setParent(top);

        Category autos = createCategory(1002, "Automobiles");
        autos.setParent(transport);

        Category cars = createCategory(1009, "Cars");
        cars.setParent(autos);

        Category toyotas = createCategory(1015, "Toyota Cars");
        toyotas.setParent(cars);

        Category dummyToyotaParent = createDummyCategory(1015);
        Category toyotaHybridsNew = createCategory(null, "Toyota Hybrid Cars");
        toyotaHybridsNew.setParent(dummyToyotaParent);

        Category dummyTruckParent = createDummyCategory(1002);
        Category editedTrucks = createCategory(1010, "All Trucks");
        editedTrucks.setParent(dummyTruckParent);

        String editedResult = null;
        String allCategoriesResult = null;
        try {
            editedResult = new String(Files.readAllBytes(Paths.get(ConsumerPactTest.class.getClassLoader().getResource("editedResult.json").toURI())));
            allCategoriesResult = new String(Files.readAllBytes(Paths.get(ConsumerPactTest.class.getClassLoader().getResource("allCategories.json").toURI())));
        } catch (Exception e) {
            System.err.println("Could not read sample json files: " + e.toString());
            return null;
        }

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Map<String, String> createdHeaders = new HashMap<>();
        createdHeaders.put("Location", "http://localhost:8081/admin/category/1020");

        try {
            return builder
                    .given("test get")
                        .uponReceiving("Retrieve a category")
                            .path("/admin/category/1015")
                            .method("GET")
                        .willRespondWith()
                            .status(200)
                            .body(mapper.writeValueAsString(toyotas))
                    .given("test get all")
                        .uponReceiving("Retrieve a category")
                            .path("/admin/category")
                            .method("GET")
                        .willRespondWith()
                            .status(200)
                    .given("test create")
                        .uponReceiving("Create a category")
                            .path("/admin/category")
                            .method("POST")
                            .headers(headers)
                            .body(mapper.writeValueAsString(toyotaHybridsNew))
                        .willRespondWith()
                            .status(201)
                            .headers(createdHeaders)
                    .given("test delete")
                        .uponReceiving("Delete a category")
                            .path("/admin/category/1013")
                            .method("DELETE")
                        .willRespondWith()
                            .status(204)
                    .given("test update")
                        .uponReceiving("Update a category")
                            .path("/admin/category/1010")
                            .method("PUT")
                            .headers(headers)
                            .body(mapper.writeValueAsString(editedTrucks))
                        .willRespondWith()
                            .status(200)
                            .body(editedResult)
                    .toPact();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected String providerName() {
        return "admin_service_provider";
    }

    @Override
    protected String consumerName() {
        return "admin_client_consumer";
    }

    @Override
    protected PactSpecVersion getSpecificationVersion() {
        return PactSpecVersion.V3;
    }

    @Override
    protected void runTest(MockServer mockServer) throws IOException {
        AdminClient client = new AdminClient(mockServer.getUrl());

        client.deleteCategory(1013);

        Category cat = client.getCategory(1015);

        Assertions.assertThat(cat).isNotNull();
        assertThat(cat.getId()).isEqualTo(1015);
        assertThat(cat.getName()).isEqualTo("Toyota Cars");
        assertThat(cat.getHeader()).isEqualTo("header");
        assertThat(cat.getImagePath()).isEqualTo("n/a");
        assertThat(cat.isVisible()).isTrue();
        assertThat(cat.getParent()).isNotNull();
        assertThat(cat.getParent().getId()).isEqualTo(1009);

        List<Category> allCategories = client.getAllCategories();

        Category parent = createDummyCategory(1015); // parent name is null because we assume client only sends parent's ID when creating
        Category newCategory = createCategory(null, "Toyota Hybrid Cars");
        newCategory.setParent(parent);
        String createdUrl = client.createCategory(newCategory);

        assertThat(createdUrl).isNotNull();
        assertThat(createdUrl).isNotEmpty();

        Category dummyTruckParent = createDummyCategory(1002);
        Category editedTrucks = createCategory(1010, "All Trucks");
        editedTrucks.setParent(dummyTruckParent);

        Category modifiedCategory = client.updateCategory(1010, editedTrucks);

        assertThat(modifiedCategory).isNotNull();
        assertThat(modifiedCategory.getId()).isEqualTo(1010);
        assertThat(modifiedCategory.getName()).isEqualTo("All Trucks");
        assertThat(modifiedCategory.getParent()).isNotNull();
        assertThat(modifiedCategory.getParent().getId()).isEqualTo(1002);

        assert(true);

    }
}
