package com.matthewcasperson.dzonezl.rest;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import org.hibernate.SessionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
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

    @RequestMapping("/image")
    public String images(@RequestParam final Map<String, String> allRequestParams) {
        /* Takes a hibernate session factory */
        final DataStore dataStore = new HibernateStore(emf.unwrap(SessionFactory.class));
        final Logger logger = new Slf4jLogger();
        final Elide elide = new Elide(logger, dataStore);

        final ElideResponse response = elide.get("/image", fromMap(allRequestParams), new Object());

        return response.getBody();
    }
}
