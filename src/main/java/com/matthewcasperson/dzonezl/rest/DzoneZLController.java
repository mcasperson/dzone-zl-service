package com.matthewcasperson.dzonezl.rest;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;

/**
 * Created by Matthew on 24/01/2016.
 */
@RestController
public class DzoneZLController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DzoneZLController.class);

    @Autowired
    private EntityManagerFactory emf;

    private MultivaluedMap<String, String> fromMap(final Map<String, String> input) {
        final MultivaluedMap queryParams = new MultivaluedMapImpl();
        for (final String key : input.keySet()) {
            queryParams.put(key, input.get(key));
        }
        return queryParams;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            value={"/data/{entity}", "/data/{entity}/relationship/{entity2}", "/data/{entity}/{child}"})
    @Transactional
    public String authors(@RequestParam final Map<String, String> allRequestParams, final HttpServletRequest request) {
        final String restOfTheUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        final SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        /* Takes a hibernate session factory */
        final DataStore dataStore = new HibernateStore(sessionFactory);
        final Logger logger = new Slf4jLogger();
        final Elide elide = new Elide(logger, dataStore);
        final MultivaluedMap<String, String> params = fromMap(allRequestParams);

        final ElideResponse response = elide.get(restOfTheUrl.replaceAll("^/data/", ""), params, new Object());

        return response.getBody();
    }
}
