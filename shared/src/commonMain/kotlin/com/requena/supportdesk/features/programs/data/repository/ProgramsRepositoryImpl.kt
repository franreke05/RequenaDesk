package com.requena.supportdesk.features.programs.data.repository

import com.requena.supportdesk.core.model.ClientProgramBillingPreview
import com.requena.supportdesk.core.model.ClientProgramRequest
import com.requena.supportdesk.core.model.ClientProgramsOverview
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.programs.data.datasource.ProgramsDataSource
import com.requena.supportdesk.features.programs.data.dto.CreateProgramRequestsRequestDto
import com.requena.supportdesk.features.programs.data.dto.DecideProgramRequestDto
import com.requena.supportdesk.features.programs.data.mapper.ProgramsMapper
import com.requena.supportdesk.features.programs.domain.repository.ProgramsRepository
import kotlinx.coroutines.CancellationException

class ProgramsRepositoryImpl(
    private val dataSource: ProgramsDataSource,
) : ProgramsRepository {
    override suspend fun getClientPrograms(): AppResult<ClientProgramsOverview> = request("No se pudieron cargar los programas.") {
        ProgramsMapper.overview(dataSource.getClientPrograms())
    }

    override suspend fun createProgramRequests(
        productKeys: Set<String>,
        customerNote: String,
    ): AppResult<List<ClientProgramRequest>> = request("No se pudo enviar la solicitud de programas.") {
        dataSource.createProgramRequests(
            CreateProgramRequestsRequestDto(productKeys = productKeys.sorted(), customerNote = customerNote),
        ).map(ProgramsMapper::request)
    }

    override suspend fun getAdminProgramRequests(): AppResult<List<ClientProgramRequest>> = request("No se pudieron cargar las solicitudes.") {
        dataSource.getAdminProgramRequests().map(ProgramsMapper::request)
    }

    override suspend fun approveProgramRequest(
        requestId: String,
        monthlyPriceCents: Long,
        adminNote: String?,
    ): AppResult<ClientProgramRequest> = request("No se pudo aprobar el programa.") {
        ProgramsMapper.request(
            dataSource.approveProgramRequest(requestId, DecideProgramRequestDto(monthlyPriceCents, adminNote)),
        )
    }

    override suspend fun rejectProgramRequest(requestId: String, adminNote: String?): AppResult<ClientProgramRequest> =
        request("No se pudo rechazar la solicitud.") {
            ProgramsMapper.request(dataSource.rejectProgramRequest(requestId, DecideProgramRequestDto(adminNote = adminNote)))
        }

    override suspend fun getBillingPreview(clientId: String, period: String): AppResult<ClientProgramBillingPreview> =
        request("No se pudo cargar la cuota mensual.") {
            ProgramsMapper.billingPreview(dataSource.getBillingPreview(clientId, period))
        }

    private suspend fun <T> request(errorMessage: String, block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        AppResult.Error(error.message ?: errorMessage, error)
    }
}
