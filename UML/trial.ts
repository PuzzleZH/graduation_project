// trial.ts
export interface patients{
    id?:number;
    position:string;
    type?:string;
}
export class hospital{
    id?:number;
    position?:'y,x';
    type?:string;
    ED_wait_list?:patients;
    ICU_wait_list?:patients;
    GW_wait_list?:patients;
    ED_list?:patients;
    ICU_list?:patients;
    GW_list?:patients;
    ED_total_bed_number?:number;
    GW_total_bed_number?:number;
    ICU_total_bed_number?:number;
    __init__(info):void
    {

    };
    __str__(self):void{

    }
    step():void
    {

    }
}
export class ambulance{
    id?:number;
    position?:number;
    position_y?:number;
    manned:patients;
    destination?:hospital;
    set_destination(hospital):boolean
    {
        return hospital;
    }
    pick_patient(patient):boolean
    {
        return patient;
    }
    move(manned,destination):boolean
    {
        if (manned != void 0) {
            return manned
        }
        else
        {
            return destination;
        }
    }
}