package ejm.adminclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/**
 * @author Ken Finnigan
 */
public class AdminClient {
    private String url;
    private ObjectMapper mapper = new ObjectMapper();

    public AdminClient(String url) {
        this.url = url;
        mapper.registerModule(new JavaTimeModule());
    }

    public List<Category> getAllCategories() throws RuntimeException, IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(url).setPath("/admin/category");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String jsonResponse =
                Request
                    .Get(uriBuilder.toString())
                    .execute()
                        .returnContent().asString();

        if (jsonResponse.isEmpty()) {
            return null;
        }

        TypeReference<List<Category>> listType = new TypeReference<List<Category>>() {};
        return mapper
                .readValue(jsonResponse, listType);
    }

    public Category getCategory(final Integer categoryId) throws IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(url).setPath("/admin/category/" + categoryId);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String jsonResponse =
                Request
                    .Get(uriBuilder.toString())
                    .execute()
                        .returnContent().asString();

        if (jsonResponse.isEmpty()) {
            return null;
        }

        return mapper
                .readValue(jsonResponse, Category.class);
    }

    public String createCategory(Category category) throws IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(url).setPath("/admin/category");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String categoryString = mapper
                .writeValueAsString(category);

        HttpEntity categoryEntity = new StringEntity(categoryString, ContentType.APPLICATION_JSON);
        HttpResponse response =
                Request
                        .Post(uriBuilder.toString())
                        .body(categoryEntity)
                        .execute().returnResponse();

        return response.getHeaders("Location")[0].getValue();
    }

    public void deleteCategory(int id) throws IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(url).setPath("/admin/category/" + id);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Request
                .Delete(uriBuilder.toString())
                .execute();
    }

    public Category updateCategory(int id, Category category) throws IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(url).setPath("/admin/category/" + id);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String categoryString = mapper
                .writeValueAsString(category);

        HttpEntity categoryEntity = new StringEntity(categoryString, ContentType.APPLICATION_JSON);
        String jsonResponse =
                Request
                        .Put(uriBuilder.toString())
                        .body(categoryEntity)
                        .execute()
                        .returnContent().asString();

        return mapper
                .readValue(jsonResponse, Category.class);
    }

}
