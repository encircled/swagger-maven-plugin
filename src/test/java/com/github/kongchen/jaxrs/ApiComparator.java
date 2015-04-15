package com.github.kongchen.jaxrs;

import java.util.Comparator;

import com.github.kongchen.swagger.docgen.mustache.MustacheApi;

/**
 * Created by chekong on 14-12-11.
 */
public class ApiComparator implements Comparator<MustacheApi> {
    @Override
    public int compare(MustacheApi o1, MustacheApi o2) {
        return o2.getPath().compareTo(o1.getPath());
    }
}
