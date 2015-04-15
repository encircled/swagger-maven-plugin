package com.github.kongchen.jaxrs.api.garage;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.github.kongchen.model.Car;
import com.github.kongchen.model.ForGeneric;
import com.github.kongchen.model.ForGeneric2;
import com.github.kongchen.model.G1;
import com.github.kongchen.model.G2;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.annotations.Authorization;
import com.wordnik.swagger.annotations.AuthorizationScope;

/**
 * Created with IntelliJ IDEA.
 * User: kongchen
 * Date: 5/13/13
 */
@Path("/garage")
@Api(value = "/garage", description = "Operations about garages", position = 3)
@Produces({ "application/json", "application/xml" })
public class GarageResourceV1 {
    @GET
    @Path("/{garageId}")
    @ApiOperation(value = "Find garages by Id", notes = "To get garage info /* <com.github.kongchen.model.G1> */",
            response = ForGeneric.class, position = 2,
            authorizations = @Authorization(value = "oauth2", scopes = { @AuthorizationScope(scope = "anything", description = "nothing") }))
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Garage not found") })
    public ForGeneric<G1> getGarageById(
            @ApiParam(value = "ID of garage that needs to be fetched", allowableValues = "range[1,100]",
                    required = true) @PathParam("garageId") String garageId)
            throws NotFoundException {
        return new ForGeneric<G1>();
    }

    @POST
    @Path("/{garageId}")
    @ApiOperation(value = "Repair a broken car in garage", notes = "To repair car /*<com.github.kongchen.model.G2,com.github.kongchen.model.v2.Car>*/",
            response = ForGeneric2.class, position = 1,
            authorizations = @Authorization(value = "oauth2", scopes = { @AuthorizationScope(scope = "anything", description = "nothing") }))
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Garage not found") })
    public ForGeneric2<G2, com.github.kongchen.model.v2.Car> getGarageById(
            @ApiParam(value = "ID of garage", allowableValues = "range[1,100]",
                    required = true) @PathParam("garageId") String garageId,
            @ApiParam(value = "broken car1", required = true) Car car)
            throws NotFoundException {
        return new ForGeneric2<G2, com.github.kongchen.model.v2.Car>();
    }
}
