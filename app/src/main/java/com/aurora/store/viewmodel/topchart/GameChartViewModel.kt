/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.viewmodel.topchart

import android.app.Application
import com.aurora.gplayapi.helpers.TopChartsHelper

class TopFreeGameChartViewModel(application: Application) : BaseChartViewModel(application) {

    init {
        type = TopChartsHelper.Type.GAME
        chart = TopChartsHelper.Chart.TOP_SELLING_FREE
        observe()
    }
}

class TopGrossingGameChartViewModel(application: Application) : BaseChartViewModel(application) {

    init {
        type = TopChartsHelper.Type.GAME
        chart = TopChartsHelper.Chart.TOP_GROSSING
        observe()
    }
}

class TrendingGameChartViewModel(application: Application) : BaseChartViewModel(application) {

    init {
        type = TopChartsHelper.Type.GAME
        chart = TopChartsHelper.Chart.MOVERS_SHAKERS
        observe()
    }
}

class TopPaidGameChartViewModel(application: Application) : BaseChartViewModel(application) {

    init {
        type = TopChartsHelper.Type.GAME
        chart = TopChartsHelper.Chart.TOP_SELLING_PAID
        observe()
    }
}

