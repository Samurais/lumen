package org.lskk.lumen.persistence.web;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;

/**
 * Created by ceefour on 13/02/2016.
 */
public class BootstrapNavigationToolbar extends NavigationToolbar {
    public BootstrapNavigationToolbar(DataTable<?, ?> table) {
        super(table);
    }

    @Override
    protected PagingNavigator newPagingNavigator(String navigatorId, DataTable<?, ?> table) {
        return new BootstrapAjaxPagingNavigator(navigatorId, table);
    }
}
