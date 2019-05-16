/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.repository
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
import com.d3.datacollector.model.State
import org.springframework.data.repository.CrudRepository

interface StateRepository : CrudRepository<State, Long>
