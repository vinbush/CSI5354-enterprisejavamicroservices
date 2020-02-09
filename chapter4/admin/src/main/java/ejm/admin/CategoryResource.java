package ejm.admin;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ejm.admin.model.Category;
import ejm.admin.model.CategoryTree;

/**
 * @author Ken Finnigan
 */
@Path("/category")
@ApplicationScoped
public class CategoryResource {

    @PersistenceContext(unitName = "AdminPU")
    private EntityManager em;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Category> all() throws Exception {
        return em.createNamedQuery("Category.findAll", Category.class)
                .getResultList();
    }

    @GET
    @Path("/tree")
    @Produces(MediaType.APPLICATION_JSON)
    public CategoryTree tree() throws Exception {
        return em.find(CategoryTree.class, 1);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(Category category) throws Exception {
        if (category.getId() != null) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity("Unable to create Category, id was already set.")
                    .build();
        }

        Category parent;
        if ((parent = category.getParent()) != null && parent.getId() != null) {
            category.setParent((Category)get(parent.getId()).getEntity());
        }

        try {
            em.persist(category);
            em.flush();
        } catch (ConstraintViolationException cve) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(cve.getMessage())
                    .build();
        } catch (Exception e) {
            return Response
                    .serverError()
                    .entity(e.getMessage())
                    .build();
        }
        return Response
                .created(new URI("category/" + category.getId().toString()))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{categoryId}")
    public Response get(@PathParam("categoryId") Integer categoryId) {
        Category found = em.find(Category.class, categoryId);
        if (found == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        }
        return Response
                .ok()
                .entity(found)
                .build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{categoryId}")
    @Transactional
    public Response remove(@PathParam("categoryId") Integer categoryId) throws Exception {
        try {
            Category entity = em.find(Category.class, categoryId);
            em.remove(entity);
        } catch (Exception e) {
            return Response
                    .serverError()
                    .entity(e.getMessage())
                    .build();
        }

        return Response
                .noContent()
                .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{categoryId}")
    @Transactional
    public Response update(@PathParam("categoryId") Integer categoryId, Category category) throws Exception {
        try {
            Category entity = em.find(Category.class, categoryId);

            if (null == entity) {
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .entity("Category with id of " + categoryId + " does not exist.")
                        .build();
            }

            if (!category.getId().equals(categoryId)) {
                 return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("Category id expected to be " + categoryId + ", but was " + category.getId() + ".")
                        .build();
            }

            Category parent;
            if ((parent = category.getParent()) != null) {
                if (parent.getId() != null && parent.getVersion() == null) {

                    category.setParent((Category)get(parent.getId()).getEntity());
                }
            }

            // all this setter logic would go in a service method, if there was a service layer
            entity.setName(category.getName());
            entity.setHeader(category.getHeader());
            entity.setImagePath(category.getImagePath());
            entity.setParent(category.getParent());
            entity.setVisible(category.isVisible());
            Category merged = em.merge(entity);

            return Response
                    .ok(merged)
                    .build();
        } catch (Exception e) {
            return Response
                    .serverError()
                    .entity(e.getMessage())
                    .build();
        }
    }
}
