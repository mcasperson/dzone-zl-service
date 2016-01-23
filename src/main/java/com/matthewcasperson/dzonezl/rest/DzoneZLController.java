package com.matthewcasperson.dzonezl.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Matthew on 24/01/2016.
 */
@RestController
public class DzoneZLController {
    @RequestMapping("/mvbdomains")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }
}
