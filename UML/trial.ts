// trial.ts
export interface patients{
    id?:number;
    position_x?:number;
    position_y?:number;
    type?:string;
}
export class hospital{
    id?:number;
    position_x?:number;
    position_y?:number;
    type?:string;
    total_bed_number?:number;
    remaining_bed_number?:number;
}
export interface ambulance{
    id?:number;
    position_x?:number;
    position_y?:number;
    manned:patients;
    destination?:hospital;
}