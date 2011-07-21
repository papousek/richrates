/*******************************************************************************
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *******************************************************************************/
package org.richfaces.examples.richrates;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Past;

import org.joda.time.DateTime;
import org.jsflot.xydata.XYDataList;
import org.jsflot.xydata.XYDataPoint;
import org.jsflot.xydata.XYDataSetCollection;
import org.richfaces.event.DropEvent;
import org.richfaces.skin.Skin;
import org.richfaces.skin.SkinFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean used on the page with chart. Is is possible to draw a chart for one currency for selected time range.
 * 
 * @author <a href="mailto:ppitonak@redhat.com">Pavol Pitonak</a>
 * @version $Revision$
 */
@ManagedBean
@ViewScoped
public class ChartBean implements Serializable {

    private static final long serialVersionUID = -1L;
    @ManagedProperty(value = "#{ratesBean.issueDate}")
    @Past(message = "To date: Future date cannot be selected.")
    private Date toDate;
    @Past(message = "From date: Future date cannot be selected.")
    private Date fromDate;
    @ManagedProperty(value = "#{ratesBean.currencies}")
    private Map<Date, Map<String, Double>> currencies;
    @ManagedProperty(value = "#{ratesBean.currencyNames}")
    private Map<String, String> currencyNames;
    private String selectedCurrency;
    private String draggedCurrency;
    private XYDataList list;
    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;
    private Logger logger;

    /**
     * Initializes class' fields.
     */
    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() {
        logger = LoggerFactory.getLogger(ChartBean.class);
        selectedCurrency = "USD";
        fromDate = new DateTime(toDate).minusDays(30).toDate();
    }

    /**
     * Getter for start date.
     * 
     * @return start date for chart
     */
    public Date getFromDate() {
        return fromDate;
    }

    /**
     * Setter for start date.
     * 
     * @param fromDate
     *            start date for chart
     */
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * Getter for end date.
     * 
     * @return end date for chart
     */
    public Date getToDate() {
        return toDate;
    }

    /**
     * Setter for end date.
     * 
     * @param toDate
     *            end date for chart
     */
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    /**
     * Getter for selected currency.
     * 
     * @return ISO code of the selected currency
     */
    public String getSelectedCurrency() {
        return selectedCurrency;
    }

    /**
     * Setter for selected currency.
     * 
     * @param selectedCurrency
     *            ISO code for selected currency
     */
    public void setSelectedCurrency(String selectedCurrency) {
        this.selectedCurrency = selectedCurrency;
    }

    /**
     * Getter for exchange rates.
     * 
     * @return map containing the mapping of dates, currency codes and exchange rates
     */
    public Map<Date, Map<String, Double>> getCurrencies() {
        return currencies;
    }

    /**
     * Setter for exchange rates.
     * 
     * @param currencies
     *            map containing the mapping of dates, currency codes and exchange rates
     */
    public void setCurrencies(Map<Date, Map<String, Double>> currencies) {
        this.currencies = currencies;
    }

    /**
     * Getter for dragged currency.
     * 
     * @return ISO code of the currency which was dragged
     */
    public String getDraggedCurrency() {
        return draggedCurrency;
    }

    /**
     * Setter for dragged currency.
     * 
     * @param draggedCurrency
     *            ISO code of the currency which was dragged and dropped
     */
    public void setDraggedCurrency(String draggedCurrency) {
        this.draggedCurrency = draggedCurrency;
    }

    /**
     * Getter for currency names, e.g. [EUR:Euro, USD:American Dollar].
     * 
     * @param currencyNames
     *            map containing a mapping of currency code to its long name
     */
    public void setCurrencyNames(Map<String, String> currencyNames) {
        this.currencyNames = currencyNames;
    }

    /**
     * Creates a data structure needed by chart component. It creates a set of points where X is date and Y is an
     * exchange rate.
     * 
     * @return set of points for chart
     */
    public XYDataSetCollection getCurrencyData() {
        Skin skin = SkinFactory.getInstance().getSkin(FacesContext.getCurrentInstance());
        XYDataSetCollection data = new XYDataSetCollection();

        if (list != null) {
            list.setColor((String) skin.getParameter(FacesContext.getCurrentInstance(), "generalLinkColor"));
            data.addDataList(list);
            return data;
        }

        list = new XYDataList();
        list.setColor((String) skin.getParameter(FacesContext.getCurrentInstance(), "generalLinkColor"));
        list.setLabel(currencyNames.get(selectedCurrency));

        DateTime toDate = new DateTime(this.toDate);
        DateTime actualDate = new DateTime(fromDate);

        while (actualDate.compareTo(toDate) <= 0) {
            if (currencies.get(actualDate.toDate()) == null) {
                actualDate = actualDate.plusDays(1);
                continue;
            }
            Number x = actualDate.getMillis();
            Number y = currencies.get(actualDate.toDate()).get(selectedCurrency).doubleValue();
            list.addDataPoint(x, y);
            actualDate = actualDate.plusDays(1);
        }

        data.addDataList(list);
        return data;
    }

    /**
     * Returns the maximal value on the Y axis so that the chart looks better.
     * 
     * @return maximal value on the Y axis
     */
    public double getYaxisMaxValue() {
        for (XYDataPoint xy : list.getDataPointList()) {
            if (xy.getY().doubleValue() < min) {
                min = xy.getY().doubleValue();
            }
            if (xy.getY().doubleValue() > max) {
                max = xy.getY().doubleValue();
            }
        }

        return max * 1.02;
    }

    /**
     * Returns the minimal value on the Y axis so that the chart looks better.
     * 
     * @return minimal value on the Y axis
     */
    public double getYaxisMinValue() {
        for (XYDataPoint xy : list.getDataPointList()) {
            if (xy.getY().doubleValue() < min) {
                min = xy.getY().doubleValue();
            }
            if (xy.getY().doubleValue() > max) {
                max = xy.getY().doubleValue();
            }
        }

        return min * 0.98;
    }

    /**
     * An action for displaying a new chart.
     */
    public void display() {
        logger.debug("Displaying a new chart");
        list = null;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
    }

    /**
     * A method used to process drop event.
     * 
     * @param event
     *            a drop event on the chart
     */
    public void processDrop(DropEvent event) {
        selectedCurrency = (String) event.getDragValue();
        display();
    }

    @AssertTrue(message = "Dates are in wrong order.")
    public boolean isDatesCorrect() {
        logger.debug("Validating the order of dates");
        return new DateTime(fromDate).isBefore(toDate.getTime());
    }
}