package com.github.kongchen.springmvc.controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import com.github.kongchen.model.Pet;
import com.github.kongchen.model.Request;
import com.github.kongchen.model.User;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kisel on 07.04.2015.
 */
@RestController
@RequestMapping("/pet")
public class PetController extends AbstractController<Pet, Long> {

    @RequestMapping(value = "/do", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Pet doSomeWork(@RequestBody List<Pet> pets) {
        return null;
    }

    @RequestMapping(value = "/{petId}", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Pet getPetById(
            @PathVariable("petId") String petId,
            @RequestHeader(value = "Accept", required = false) MediaType accept) {
        return new Pet();
    }

    @RequestMapping(value = "/{petId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public void deletePetById(
            @PathVariable("petId") String petId) {
        System.out.println(petId);
    }

    public <T extends Number> List<T> test2() {
        return null;
    }

    public <T extends Long> T test3() {
        return null;
    }

    public Long test4() {
        return null;
    }

    public void test5() {
    }

    public <T extends Number> List<Set<T>> test6() {
        return null;
    }

    public <T extends Number> List<? extends Collection<T>> test7() {
        return null;
    }

    @RequestMapping("/request")
    @ResponseStatus(HttpStatus.OK)
    public List<Pet> testRequestWrapper(@RequestBody Request<List<Pet>> petsRequest) {
        return null;
    }

    @RequestMapping("/wildcard")
    @ResponseStatus(HttpStatus.OK)
    public List<? extends Pet> testRequestWrapperWildcard(@RequestBody Request<List<? extends Pet>> petsRequest) {
        return null;
    }

    @RequestMapping("/maptest")
    @ResponseStatus(HttpStatus.OK)
    public Map<User, List<DateTime>> testMap(@RequestBody Map<User, List<DateTime>> map) {
        return null;
    }

    @RequestMapping("/compex_generic")
    @ResponseStatus(HttpStatus.OK)
    public Map<List<Long>, Map<String, List<Set<String>>>> complexGenericTest(@RequestBody Map<List<Long>, Map<String, List<Set<String>>>> some) {
        return null;
    }

}
